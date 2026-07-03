package com.studyflow.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    @Test
    void successWrapsDataWithZeroCode() {
        ApiResponse<Map<String, String>> response = ApiResponse.success(Map.of("name", "StudyFlow"));

        assertThat(response.code()).isEqualTo(0);
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).containsEntry("name", "StudyFlow");
    }

    @Test
    void errorWrapsCodeAndMessageWithNullData() {
        ApiResponse<Void> response = ApiResponse.error(400, "参数错误");

        assertThat(response.code()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("参数错误");
        assertThat(response.data()).isNull();
    }
}
