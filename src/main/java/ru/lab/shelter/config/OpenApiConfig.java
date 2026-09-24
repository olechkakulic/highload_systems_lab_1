package ru.lab.shelter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI shelterOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Платформа приютов — ЛР №1")
                .version("1.0.0")
                .description(
                    "Монолит. Профили и роли описывают предметную область; аутентификация и"
                        + " проверка прав относятся к ЛР №3. Все списки ограничены 50 записями."));
  }
}
