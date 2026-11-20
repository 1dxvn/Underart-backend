package com.underart.infrastructure.web.controller;

import com.underart.application.dto.OrderResponse;
import com.underart.application.usecase.OrderConversationService;
import com.underart.domain.port.out.OrderMessageRepository.Message;
import com.underart.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders/{id}")
@RequiredArgsConstructor
public class OrderConversationController {
    private final CurrentUserProvider currentUser;
    private final OrderConversationService conversations;

    @GetMapping("/messages")
    public List<Message> messages(@PathVariable UUID id, @RequestParam(required = false) Long after) {
        return conversations.list(id, currentUser.currentUserId(), after);
    }

    @PostMapping("/messages")
    public Message send(@PathVariable UUID id, @Valid @RequestBody MessageRequest request) {
        return conversations.send(id, currentUser.currentUserId(), request.message());
    }

    @PostMapping("/external-payment/start")
    public OrderResponse start(@PathVariable UUID id) {
        return OrderResponse.from(conversations.start(id, currentUser.currentUserId()));
    }

    @PostMapping("/external-payment/report")
    public OrderResponse report(@PathVariable UUID id) {
        return OrderResponse.from(conversations.report(id, currentUser.currentUserId()));
    }

    @PostMapping("/external-payment/review")
    public OrderResponse review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest request) {
        return OrderResponse.from(conversations.review(id, currentUser.currentUserId(), request.received()));
    }

    public record MessageRequest(@NotBlank @Size(max = 2000) String message) {}
    public record ReviewRequest(@NotNull Boolean received) {}
}
