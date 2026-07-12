package com.studyflow.media;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MediaTranscodePlaylistParserTest {
    private final MediaTranscodePlaylistParser parser = new MediaTranscodePlaylistParser();

    @Test
    void parsesHlsSegmentsWithDurationsInOrder() throws Exception {
        Path playlist = Files.createTempFile("ruru-hls-", ".m3u8");
        Files.writeString(playlist, """
                #EXTM3U
                #EXT-X-VERSION:3
                #EXT-X-TARGETDURATION:6
                #EXT-X-MEDIA-SEQUENCE:0
                #EXTINF:6.000,
                segment-000.ts
                #EXTINF:5.240,
                segment-001.ts
                #EXT-X-ENDLIST
                """);

        var segments = parser.parse(playlist);

        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).index()).isEqualTo(0);
        assertThat(segments.get(0).durationSeconds()).isEqualByComparingTo("6.000");
        assertThat(segments.get(0).filename()).isEqualTo("segment-000.ts");
        assertThat(segments.get(1).index()).isEqualTo(1);
        assertThat(segments.get(1).durationSeconds()).isEqualByComparingTo("5.240");
        assertThat(segments.get(1).filename()).isEqualTo("segment-001.ts");
    }
}
