package com.studyflow.media;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class MediaTranscodePlaylistParser {
    public List<ParsedSegment> parse(Path playlistPath) {
        try {
            List<String> lines = Files.readAllLines(playlistPath);
            List<ParsedSegment> segments = new ArrayList<>();
            BigDecimal pendingDuration = null;
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("#EXTINF:")) {
                    pendingDuration = parseDuration(line);
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                if (pendingDuration != null) {
                    segments.add(new ParsedSegment(segments.size(), pendingDuration, line));
                    pendingDuration = null;
                }
            }
            return segments;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read HLS playlist: " + playlistPath, exception);
        }
    }

    private BigDecimal parseDuration(String line) {
        String value = line.substring("#EXTINF:".length()).replace(",", "").trim();
        return new BigDecimal(value);
    }

    public record ParsedSegment(
            int index,
            BigDecimal durationSeconds,
            String filename
    ) {
    }
}
