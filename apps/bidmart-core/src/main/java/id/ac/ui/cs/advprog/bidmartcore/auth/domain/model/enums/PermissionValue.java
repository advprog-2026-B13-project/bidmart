package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermissionValue {
    AUCTION_CREATE("auction:create"),
    AUCTION_DELETE("auction:delete"),
    ACCOUNT_DEACTIVATE("account:deactivate"),
    USER_MANAGE("user:manage")
    ;

    private final String permissionName;

    public static PermissionValue fromName(String name) {
        for (PermissionValue perms : PermissionValue.values()) {
            if (perms.permissionName.equals(name)) {
                return perms;
            }
        }

        throw new IllegalArgumentException("Unknown Permission Name: " + name);
    }
}
