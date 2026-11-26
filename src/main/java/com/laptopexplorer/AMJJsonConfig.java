package com.laptopexplorer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** La aplicación no incluye Spring Web, así que el ObjectMapper se declara a mano. */
@Configuration
public class AMJJsonConfig {

    @Bean
    public ObjectMapper amjObjectMapper() {
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
