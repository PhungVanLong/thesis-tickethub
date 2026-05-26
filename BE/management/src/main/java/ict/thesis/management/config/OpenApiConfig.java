package ict.thesis.management.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI managementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Management API")
                        .description("API documentation for the management service")
                        .version("v1"))
                .externalDocs(new ExternalDocumentation()
                        .description("Project documentation")
                        .url("/doc.md"));
    }
}

