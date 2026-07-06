package com.studyflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI studyFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("StudyFlow API")
                        .description("StudyFlow \u5b66\u4e60\u4efb\u52a1\u7ba1\u7406\u7cfb\u7edf\u63a5\u53e3\u6587\u6863")
                        .version("v1.0.0"));
    }
}
