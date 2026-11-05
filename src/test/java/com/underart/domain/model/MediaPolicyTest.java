package com.underart.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.underart.domain.exception.BusinessRuleException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MediaPolicyTest {

    @Test
    void detectsKindIgnoringCaseAndParameters() {
        assertThat(MediaPolicy.kindOf("IMAGE/JPEG")).contains(MediaKind.IMAGE);
        assertThat(MediaPolicy.kindOf("video/mp4; codecs=avc1")).contains(MediaKind.VIDEO);
        assertThat(MediaPolicy.kindOf("application/pdf")).isEmpty();
        assertThat(MediaPolicy.kindOf(null)).isEmpty();
    }

    @Test
    void appliesSizeLimitsPerKindAndUploadMode() {
        long video = 100L * 1024 * 1024;
        assertThat(MediaPolicy.requireAllowed("video/mp4", video, true)).isEqualTo(MediaKind.VIDEO);
        assertThatThrownBy(() -> MediaPolicy.requireAllowed("video/mp4", video, false))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> MediaPolicy.requireAllowed("image/png", MediaPolicy.MAX_IMAGE_BYTES + 1, true))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> MediaPolicy.requireAllowed("image/png", 0, false))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void buildsStorageKeyWithExtension() {
        UUID artwork = UUID.randomUUID();
        UUID media = UUID.randomUUID();
        assertThat(MediaPolicy.storageKey(artwork, media, "video/quicktime"))
                .isEqualTo("artworks/" + artwork + "/" + media + ".mov");
    }
}
