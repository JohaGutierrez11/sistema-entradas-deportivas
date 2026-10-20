package ec.edu.uce.tickets.match.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI matchServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("match-service API")
                .version("v1")
                .description("Estadios, secciones, asientos y partidos del Sistema de Gestión de Entradas"));
    }
}