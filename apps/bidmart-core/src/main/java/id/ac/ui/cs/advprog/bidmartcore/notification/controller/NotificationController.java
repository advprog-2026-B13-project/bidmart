package id.ac.ui.cs.advprog.bidmartcore.notification.controller;

import id.ac.ui.cs.advprog.bidmartcore.notification.model.NotificationModel;
import id.ac.ui.cs.advprog.bidmartcore.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// TODO: rename atau modif file template ini
@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/all")
    public Map<String, Object> getAll() {
        List<NotificationModel> notificationDatas = notificationService.findAll();

        return Map.of(
                "results", notificationDatas
        );
    }
}
