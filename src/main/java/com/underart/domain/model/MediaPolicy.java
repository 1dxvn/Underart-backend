package com.underart.domain.model;

import com.underart.domain.exception.BusinessRuleException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MediaPolicy {

    public static final int MAX_MEDIA_PER_ARTWORK = 15;
    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    public static final long MAX_VIDEO_BYTES_PROXIED = 50L * 1024 * 1024;
    public static final long MAX_VIDEO_BYTES_DIRECT = 200L * 1024 * 1024;

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp",
            "video/mp4", "mp4", "video/webm", "webm", "video/quicktime", "mov");

    private MediaPolicy() {
    }

    public static String normalize(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    public static Optional<MediaKind> kindOf(String contentType) {
        String type = normalize(contentType);
        if (!EXTENSIONS.containsKey(type)) {
            return Optional.empty();
        }
        return Optional.of(type.startsWith("image/") ? MediaKind.IMAGE : MediaKind.VIDEO);
    }

    public static long maxBytes(MediaKind kind, boolean directUpload) {
        if (kind == MediaKind.IMAGE) {
            return MAX_IMAGE_BYTES;
        }
        return directUpload ? MAX_VIDEO_BYTES_DIRECT : MAX_VIDEO_BYTES_PROXIED;
    }

    public static MediaKind requireAllowed(String contentType, long sizeBytes, boolean directUpload) {
        MediaKind kind = kindOf(contentType).orElseThrow(() -> new BusinessRuleException(
                "Unsupported file type. Use JPEG, PNG, WEBP, MP4, WEBM or MOV"));
        if (sizeBytes <= 0) {
            throw new BusinessRuleException("The file is empty");
        }
        long max = maxBytes(kind, directUpload);
        if (sizeBytes > max) {
            throw new BusinessRuleException("The file exceeds the maximum of " + (max / (1024 * 1024)) + " MB");
        }
        return kind;
    }

    public static boolean hasImageSignature(String contentType, byte[] bytes) {
        String header = HexFormat.of().formatHex(bytes, 0, Math.min(bytes.length, 12));
        return switch (normalize(contentType)) {
            case "image/jpeg" -> header.startsWith("ffd8ff");
            case "image/png" -> header.startsWith("89504e470d0a1a0a");
            case "image/webp" -> header.startsWith("52494646") && header.endsWith("57454250") && bytes.length >= 12;
            default -> false;
        };
    }

    public static String storageKey(UUID artworkId, UUID mediaId, String contentType) {
        return "artworks/" + artworkId + "/" + mediaId + "." + EXTENSIONS.get(normalize(contentType));
    }
}
