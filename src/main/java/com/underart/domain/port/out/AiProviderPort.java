package com.underart.domain.port.out;

import com.underart.domain.model.AiResult;
import com.underart.domain.model.ArtworkDraft;
import java.util.List;

public interface AiProviderPort {

    AiResult valuateArtwork(String title, String technique, String dimensions);

    ArtworkDraft suggestArtwork(String title, String notes, String technique, String dimensions, String currency);

    List<String> generateTags(String title, String description);

    String getProviderName();
}
