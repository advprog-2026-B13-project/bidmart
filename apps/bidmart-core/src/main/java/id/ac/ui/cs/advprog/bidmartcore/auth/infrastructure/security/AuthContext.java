package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Set;
import java.util.UUID;

@Component
@RequestScope
@Getter
@Setter
public class AuthContext {
    private UUID userId;
    private String sessionId;
    private User user;
    private Set<PermissionValue> permissions;
    private boolean authenticated;

    public boolean hasPermission(PermissionValue permission) {
        return permissions != null && permissions.contains(permission);
    }
}

