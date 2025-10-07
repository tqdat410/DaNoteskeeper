package app.notekeeper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.version}")
    private String applicationVersion;

    @Value("${server.port}")
    private String serverPort;

    @Value("${app.deployment-url}")
    private String deploymentUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .servers(java.util.Arrays.asList(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local server"),
                        new Server()
                                .url(deploymentUrl)
                                .description("Deployment server")))
                .info(new Info()
                        .title(applicationName.toUpperCase() + " API")
                        .description("AI-powered note keeper application for advanced storing and sharing notes")
                        .version(applicationVersion))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT")));
    }
}
