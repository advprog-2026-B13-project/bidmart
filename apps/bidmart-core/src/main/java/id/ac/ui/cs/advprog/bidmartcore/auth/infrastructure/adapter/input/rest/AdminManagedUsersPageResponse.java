package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.AdminRolePermissionUseCase.UsersForRoleManagementPageView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated users payload for admin role management")
public class AdminManagedUsersPageResponse {

    @Schema(description = "User rows in current page")
    private List<AdminManagedUserResponse> users;

    @Schema(description = "Current page index", example = "0")
    private int page;

    @Schema(description = "Page size", example = "20")
    private int size;

    @Schema(description = "Total available users", example = "185")
    private long totalElements;

    @Schema(description = "Total pages", example = "10")
    private int totalPages;

    @Schema(description = "Whether next page exists", example = "true")
    private boolean hasNext;

    public static AdminManagedUsersPageResponse fromView(UsersForRoleManagementPageView view) {
        List<AdminManagedUserResponse> users = view.users().stream()
                .map(AdminManagedUserResponse::fromView)
                .toList();

        return new AdminManagedUsersPageResponse(
                users,
                view.page(),
                view.size(),
                view.totalElements(),
                view.totalPages(),
                view.hasNext()
        );
    }
}

