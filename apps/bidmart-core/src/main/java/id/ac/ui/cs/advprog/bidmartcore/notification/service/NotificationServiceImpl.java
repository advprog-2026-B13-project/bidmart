package id.ac.ui.cs.advprog.bidmartcore.notification.service;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.NotificationModel;
import id.ac.ui.cs.advprog.bidmartcore.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// TODO: rename atau modif file template ini
@Service
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public List<NotificationModel> findAll() {
        return notificationRepository.findAll();
    }
}
