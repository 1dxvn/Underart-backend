package com.underart.application.dto;

import com.underart.domain.port.in.DirectMediaUploadUseCase.UploadTicket;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MediaUploadUrlResponse(
        UUID mediaId,
        String uploadUrl,
        String method,
        Map<String, String> requiredHeaders,
        Instant expiresAt) {

    public static MediaUploadUrlResponse from(UploadTicket ticket) {
        return new MediaUploadUrlResponse(ticket.mediaId(), ticket.uploadUrl(), "PUT",
                ticket.requiredHeaders(), ticket.expiresAt());
    }
}
