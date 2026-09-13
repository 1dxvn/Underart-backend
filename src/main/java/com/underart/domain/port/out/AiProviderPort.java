package com.underart.domain.port.out;

import com.underart.domain.model.AiResult;
import java.util.List;

public interface AiProviderPort {

    AiResult valuateArtwork(String title, String technique, String dimensions);

    List<String> generateTags(String title, String description);

    String getProviderName();
}
