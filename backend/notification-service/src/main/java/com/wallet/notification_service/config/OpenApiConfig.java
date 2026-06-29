package com.wallet.notification_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VaultX Notification Service API")
                        .description("Kafka consumer service for real-time wallet notifications and email alerts")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("VaultX Team")
                                .email("dev@vaultx.com")));
    }
}
