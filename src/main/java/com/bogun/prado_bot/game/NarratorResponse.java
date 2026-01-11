package com.bogun.prado_bot.game;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NarratorResponse(Scene scene, List<Choice> choices,
                               @JsonProperty("memory_update") MemoryUpdate memoryUpdate) {

    public record Scene(String title, String text, @JsonProperty("location_line") String locationLine) {}

    public record Choice(String id, String label, String intent, String note) {}

    public record MemoryUpdate(@JsonProperty("add_facts") List<String> addFacts,
                               @JsonProperty("remove_facts") List<String> removeFacts,
                               List<String> npcs) {}
}
