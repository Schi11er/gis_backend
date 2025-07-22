package com.gisbackend.buildingstreamer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfiguration {

    @Value("${KAFKA_BROKER_URL}")
    private String kafkaBrokerUrl;

    @Value("${BUILDING_STREAMER_SERVER_PORT}")
    private String serverPort;
    
    @Bean
    public OpenAPI defineOpenApi() {
        Server server = new Server();
        server.setUrl("http://" + kafkaBrokerUrl + ":" + serverPort);
        server.setDescription("Local development server");

        Contact contact = new Contact();
        contact.setName("Sebastian Schilling");
        contact.setEmail("sebastian.schilling@htw-dresden.de");

        Info info = new Info();
        info.setTitle("Building Information API");
        info.setVersion("1.0");
        info.setDescription("API for managing building information");

        return new OpenAPI()
            .addServersItem(server)
            .info(info);
    }
}
