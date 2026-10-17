package com.underart.infrastructure.ai.template;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AbstractAiAdapterTest {

    @Test
    void extractsJsonFromMarkdownFences() {
        String raw = "Claro, aquí está:\n```json\n{\"suggestedPrice\": 350, \"tags\": [\"óleo\"]}\n```";
        assertThat(AbstractAiAdapter.extractJson(raw)).isEqualTo("{\"suggestedPrice\": 350, \"tags\": [\"óleo\"]}");
    }

    @Test
    void extractsArrayWhenItComesFirst() {
        assertThat(AbstractAiAdapter.extractJson("Etiquetas: [\"a\", \"b\"] listo")).isEqualTo("[\"a\", \"b\"]");
    }

    @Test
    void returnsTextUnchangedWithoutJson() {
        assertThat(AbstractAiAdapter.extractJson("sin json")).isEqualTo("sin json");
        assertThat(AbstractAiAdapter.extractJson(null)).isEmpty();
    }
}
