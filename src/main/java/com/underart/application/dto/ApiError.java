package com.underart.application.dto;

import java.time.Instant;

public record ApiError(int status, String message, Instant timestamp) {
}
