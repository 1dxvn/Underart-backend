package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.port.out.NotificationRepositoryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationsServiceTest {

    private final NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);
    private final NotificationsService service = new NotificationsService(repository);
    private final UUID userId = UUID.randomUUID();

    @Test
    void markReadSucceedsForOwnNotifications() {
        when(repository.markRead("n1", userId)).thenReturn(true);
        assertThatCode(() -> service.markRead(userId, "n1")).doesNotThrowAnyException();
    }

    @Test
    void markReadOfForeignOrMissingNotificationIs404() {
        when(repository.markRead("ajena", userId)).thenReturn(false);
        assertThatThrownBy(() -> service.markRead(userId, "ajena")).isInstanceOf(ResourceNotFoundException.class);
    }
}
