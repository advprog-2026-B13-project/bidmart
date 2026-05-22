package id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermissionValue {
    AUCTION_CREATE("auction:create"),
    AUCTION_DELETE("auction:delete"),

    CATALOG_CREATE_CATEGORY("catalog:create_category"),
    CATALOG_UPDATE_CATEGORY("catalog:update_category"),
    CATALOG_DELETE_CATEGORY("catalog:delete_category"),

    LISTING_CREATE_LISTING("listing:create_listing"),
    LISTING_UPDATE_ALL_LISTING("listing:update_listing"),
    LISTING_DELETE_ALL_LISTING("listing:delete_listing"),

    ORDER_UPDATE_SHIPMENT_STATUS("order:update_shipment_status"),
    ORDER_CONFIRM_DELIVERY("order:confirm_delivery"),

    ACCOUNT_DEACTIVATE("account:deactivate"),

    USER_MANAGE("user:manage"),

    ADMIN("admin:all")
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
