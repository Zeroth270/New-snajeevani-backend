package com.anvesha.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI anveshaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Anvesha API")
                        .description("Molecule novelty & patent-urgency tracking platform for Indian research institutions")
                        .version("1.0.0")
                        .contact(new Contact().name("Anvesha Team")));
    }
}
