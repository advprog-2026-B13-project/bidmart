package id.ac.ui.cs.advprog.bidmartcore.notification.repository;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.NotificationModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO: rename atau modif file template ini
@Component
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {
    private final NotificationSpringRepository springRepository;

    @Override
    public List<NotificationModel> findAll() {
        return springRepository.findAll();
    }
}
