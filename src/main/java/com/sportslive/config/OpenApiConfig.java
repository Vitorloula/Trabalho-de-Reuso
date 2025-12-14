package com.sportslive.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sports Live Service API")
                        .version("1.0")
                        .description("API para acesso unificado a dados de eventos esportivos em tempo real"))
                .servers(List.of(
                        new Server()
                                .url("https://gleaming-worm-vitor-project-201f7f66.koyeb.app")
                                .description("Koyeb Production")
                ));
    }
}
