package com.studyflow.live;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LiveMessageRequest(
        @NotBlank @Size(max = 500) String content,
        String color,
        String type
) {
    public String color() {
        return color == null || color.isBlank() ? "#ffffff" : color.trim();
    }

    public String type() {
        if (type == null || type.isBlank()) {
            return "CHAT";
        }
        String trimmed = type.trim().toUpperCase();
        return switch (trimmed) {
            case "CHAT", "DANMAKU", "SYSTEM" -> trimmed;
            default -> "CHAT";
        };
    }
}
