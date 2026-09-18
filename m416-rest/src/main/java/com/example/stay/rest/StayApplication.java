package com.example.stay.rest;

import com.example.stay.infrastructure.common.config.IntegrationConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(IntegrationConfiguration.class)
public class StayApplication {
    public static void main(String[] args) { SpringApplication.run(StayApplication.class, args); }
}
