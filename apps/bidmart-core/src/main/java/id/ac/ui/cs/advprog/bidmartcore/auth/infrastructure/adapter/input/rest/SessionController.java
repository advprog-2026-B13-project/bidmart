package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.SessionUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/sessions")
@RequireLogin
@RequiredArgsConstructor
@Tag(name = "Session Management", description = "List, revoke, and manage active sessions")
public class SessionController {

    private final SessionUseCase sessionUseCase;
    private final AuthContext authContext;

    @GetMapping
    @Operation(
            summary = "List all sessions",
            description = "Returns all sessions for the current user, including active/inactive status and which one is the current session."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sessions retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listSessions() {
        List<Session> sessions = sessionUseCase.listSessions(authContext.getUserId());
        List<Map<String, Object>> result = sessions.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", s.getId());
            map.put("isActive", s.isActive());
            map.put("expiresAt", s.getExpiresAt().toString());
            map.put("isCurrent", s.getId().equals(authContext.getSessionId()));
            return map;
        }).toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(
            summary = "Revoke a specific session",
            description = "Deactivates the specified session. The session must belong to the current user."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session revoked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Session not found or doesn't belong to user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @Parameter(description = "ID of the session to revoke") @PathVariable String sessionId) {
        try {
            sessionUseCase.revokeSession(authContext.getUserId(), sessionId);
            return ResponseEntity.ok(ApiResponse.success("Session revoked", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping
    @Operation(
            summary = "Revoke all other sessions",
            description = "Deactivates all sessions for the current user except the current one. Useful for 'sign out everywhere else'."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All other sessions revoked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Void>> revokeAllOtherSessions() {
        sessionUseCase.revokeAllOtherSessions(authContext.getUserId(), authContext.getSessionId());
        return ResponseEntity.ok(ApiResponse.success("All other sessions revoked", null));
    }
}
