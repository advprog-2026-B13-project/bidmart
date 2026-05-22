package id.ac.ui.cs.advprog.bidmartcore.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDTO {
    private boolean emailEnabled;
    private boolean pushEnabled;
}