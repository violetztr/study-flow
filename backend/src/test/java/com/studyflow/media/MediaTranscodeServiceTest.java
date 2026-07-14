package com.studyflow.media;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MediaTranscodeService} internal validation, utility methods,
 * and variant configuration — no database, S3, or FFmpeg needed.
 */
class MediaTranscodeServiceTest {

    private static final MediaTranscodeService SERVICE = new MediaTranscodeService(
            null, null, null, null, null, null, null
    );

    // ──────────────────────────────────────────────
    // VARIANTS
    // ──────────────────────────────────────────────

    @Test
    void variantsShouldContainExactly480p720p1080p() {
        var variants = variantsField();

        assertThat(variants).hasSize(3);

        List<String> labels = variants.stream()
                .map(VariantInfo::qualityLabel)
                .toList();
        assertThat(labels).containsExactly("480P", "720P", "1080P");
    }

    @Test
    void variantsShouldBeOrderedByResolutionAscending() {
        var variants = variantsField();

        List<Integer> widths = variants.stream()
                .map(VariantInfo::width)
                .toList();
        assertThat(widths).isSorted();
    }

    @Test
    void variantWidthsShouldMatchExpectedResolutions() {
        var variants = variantsField();

        assertThat(variants.get(0).width()).isEqualTo(640);
        assertThat(variants.get(1).width()).isEqualTo(1280);
        assertThat(variants.get(2).width()).isEqualTo(1920);
    }

    // ──────────────────────────────────────────────
    // extension()
    // ──────────────────────────────────────────────

    @Test
    void extensionShouldRecognizeLowercaseExtensions() {
        assertThat(extension("video.mp4", null)).isEqualTo("mp4");
        assertThat(extension("movie.webm", null)).isEqualTo("webm");
        assertThat(extension("clip.mov", null)).isEqualTo("mov");
        assertThat(extension("film.mkv", null)).isEqualTo("mkv");
    }

    @Test
    void extensionShouldRecognizeUppercaseExtensions() {
        assertThat(extension("VIDEO.MP4", null)).isEqualTo("mp4");
        assertThat(extension("MOVIE.WEBM", null)).isEqualTo("webm");
    }

    @Test
    void extensionShouldFallbackToContentTypeWhenNoRecognizedExtension() {
        assertThat(extension("noext", "video/mp4")).isEqualTo("mp4");
        assertThat(extension("noext", "video/webm")).isEqualTo("webm");
        assertThat(extension("unknown.bin", "video/mp4")).isEqualTo("mp4");
    }

    @Test
    void extensionShouldDefaultToMp4ForUnknownTypes() {
        assertThat(extension("unknown.bin", "application/octet-stream")).isEqualTo("mp4");
        assertThat(extension("file.xyz", null)).isEqualTo("mp4");
    }

    @Test
    void extensionShouldHandleNullFilename() {
        assertThat(extension(null, "video/mp4")).isEqualTo("mp4");
    }

    // ──────────────────────────────────────────────
    // trimError()
    // ──────────────────────────────────────────────

    @Test
    void trimErrorShouldReturnNullMessageAsDefault() {
        assertThat(trimError(null)).isEqualTo("unknown transcode error");
    }

    @Test
    void trimErrorShouldReturnBlankMessageAsDefault() {
        assertThat(trimError("   ")).isEqualTo("unknown transcode error");
        assertThat(trimError("")).isEqualTo("unknown transcode error");
    }

    @Test
    void trimErrorShouldReturnNormalMessageUnchanged() {
        assertThat(trimError("FFmpeg not found")).isEqualTo("FFmpeg not found");
    }

    @Test
    void trimErrorShouldTruncateLongMessages() {
        String longMessage = "X".repeat(1500);
        String result = trimError(longMessage);
        assertThat(result).hasSize(1000);
        assertThat(result).endsWith("X");
    }

    // ──────────────────────────────────────────────
    // normalizeQualityPath()
    // ──────────────────────────────────────────────

    @Test
    void normalizeQualityPathShouldLowercaseAndTrim() {
        assertThat(normalizeQualityPath("480P")).isEqualTo("480p");
        assertThat(normalizeQualityPath("720P")).isEqualTo("720p");
        assertThat(normalizeQualityPath("1080P")).isEqualTo("1080p");
        assertThat(normalizeQualityPath(" 480P ")).isEqualTo("480p");
    }

    // ──────────────────────────────────────────────
    // Reflection helpers
    // ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static List<VariantInfo> variantsField() {
        try {
            Field field = MediaTranscodeService.class.getDeclaredField("VARIANTS");
            field.setAccessible(true);
            List<?> raw = (List<?>) field.get(null);
            return raw.stream().map(VariantInfo::from).toList();
        } catch (Exception e) {
            throw new AssertionError("Failed to read VARIANTS field", e);
        }
    }

    private static String extension(String filename, String contentType) {
        return (String) invoke("extension", new Class[]{String.class, String.class}, filename, contentType);
    }

    private static String trimError(String message) {
        return (String) invoke("trimError", new Class[]{String.class}, message);
    }

    private static String normalizeQualityPath(String qualityLabel) {
        return (String) invoke("normalizeQualityPath", new Class[]{String.class}, qualityLabel);
    }

    private static Object invoke(String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method method = MediaTranscodeService.class.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(SERVICE, args);
        } catch (Exception e) {
            throw new AssertionError("Failed to invoke " + methodName, e);
        }
    }

    // ──────────────────────────────────────────────
    // Data holder: reads private VariantSpec record via RecordComponent
    // ──────────────────────────────────────────────

    private record VariantInfo(String qualityLabel, int width, int height, int bitrateKbps) {
        static VariantInfo from(Object variant) {
            try {
                var components = variant.getClass().getRecordComponents();
                String label = (String) components[0].getAccessor().invoke(variant);
                int width = (int) components[1].getAccessor().invoke(variant);
                int height = (int) components[2].getAccessor().invoke(variant);
                int bitrateKbps = (int) components[3].getAccessor().invoke(variant);
                return new VariantInfo(label, width, height, bitrateKbps);
            } catch (Exception e) {
                throw new AssertionError("Failed to extract VariantInfo", e);
            }
        }
    }
}
