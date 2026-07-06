package com.studyflow.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiDocumentationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesKnife4jDocumentationPage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/doc.html", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Knife4j");
    }

    @Test
    void exposesOpenApiMetadata() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"title\":\"StudyFlow API\"")
                .contains("StudyFlow \u5b66\u4e60\u4efb\u52a1\u7ba1\u7406\u7cfb\u7edf\u63a5\u53e3\u6587\u6863");
    }
}
