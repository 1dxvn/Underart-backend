package com.underart.infrastructure.web.controller;

import com.underart.application.dto.CountResponse;
import com.underart.application.dto.NotificationResponse;
import com.underart.application.dto.PageResponse;
import com.underart.domain.model.PageQuery;
import com.underart.domain.port.in.NotificationsUseCase;
import com.underart.infrastructure.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Notificaciones", description = "Bandeja de notificaciones del usuario autenticado")
@RequestMapping("/api/v1/users/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final CurrentUserProvider currentUser;
    private final NotificationsUseCase notificationsUseCase;

    @GetMapping
    @Operation(summary = "Listar notificaciones, opcionalmente solo las no leídas")
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(notificationsUseCase.list(currentUser.currentUserId(), unreadOnly,
                PageQuery.of(page, size)), NotificationResponse::from));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Número de notificaciones sin leer")
    public ResponseEntity<CountResponse> unreadCount() {
        return ResponseEntity.ok(new CountResponse(notificationsUseCase.unreadCount(currentUser.currentUserId())));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Marcar una notificación como leída")
    public ResponseEntity<Void> markRead(@PathVariable String id) {
        notificationsUseCase.markRead(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Operation(summary = "Marcar todas como leídas")
    public ResponseEntity<Void> markAllRead() {
        notificationsUseCase.markAllRead(currentUser.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
