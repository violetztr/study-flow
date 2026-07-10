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
                        .title("Ruru Community API")
                        .description("Ruru \u793e\u533a\u63a5\u53e3\u6587\u6863")
                        .version("v1.0.0"));
    }
}
