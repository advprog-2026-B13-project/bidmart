package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.ProfileUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "View and update user profile, account deactivation")
public class ProfileController {

    private final ProfileUseCase profileUseCase;
    private final AuthContext authContext;

    @GetMapping
    @RequireLogin
    @Operation(
            summary = "Get current user profile",
            description = "Returns the authenticated user's profile including display name, photo URL, shipping address, role, status, and 2FA settings."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile() {
        User user = profileUseCase.getProfile(authContext.getUserId());
        return ResponseEntity.ok(ApiResponse.success(toProfileMap(user)));
    }

    @PutMapping
    @RequireLogin
    @Operation(
            summary = "Update current user profile",
            description = "Updates the authenticated user's profile fields. Only non-null fields in the request body will be updated."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(@RequestBody ProfileUpdateRequest request) {
        try {
            User user = profileUseCase.updateProfile(
                    authContext.getUserId(),
                    request.getDisplayName(),
                    request.getPhotoUrl(),
                    request.getShippingAddress()
            );
            return ResponseEntity.ok(ApiResponse.success("Profile updated", toProfileMap(user)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/deactivate/{targetUserId}")
    @RequirePermission(PermissionValue.ACCOUNT_DEACTIVATE)
    @Operation(
            summary = "Deactivate a user account",
            description = "Suspends the target user account and revokes all their active sessions. Requires the `account:deactivate` permission."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account deactivated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — missing permission")
    })
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @Parameter(description = "UUID of the user account to deactivate") @PathVariable UUID targetUserId) {
        try {
            profileUseCase.deactivateAccount(targetUserId);
            return ResponseEntity.ok(ApiResponse.success("Account deactivated", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private Map<String, Object> toProfileMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("email", user.getEmail());
        map.put("displayName", user.getDisplayName());
        map.put("photoUrl", user.getPhotoUrl());
        map.put("shippingAddress", user.getShippingAddress());
        map.put("status", user.getStatus().name());
        map.put("default2FAMethod", user.getDefault2FAMethod().name());
        map.put("createdAt", user.getCreatedAt().toString());
        if (user.getRole() != null) {
            map.put("role", user.getRole().getName());
        }
        return map;
    }
}
