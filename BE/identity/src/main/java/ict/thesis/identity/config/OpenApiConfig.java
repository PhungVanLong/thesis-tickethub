package ict.thesis.identity.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI identityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Identity API")
                        .description("API documentation for the identity service")
                        .version("v1"))
                .externalDocs(new ExternalDocumentation()
                        .description("Project documentation")
                        .url("/DOC_API_FE.md"));
    }
}

