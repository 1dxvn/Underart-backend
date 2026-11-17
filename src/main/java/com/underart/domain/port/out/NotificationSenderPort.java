package com.underart.domain.port.out;

import com.underart.domain.model.Notification;

public interface NotificationSenderPort {

    void send(Notification notification);
}
