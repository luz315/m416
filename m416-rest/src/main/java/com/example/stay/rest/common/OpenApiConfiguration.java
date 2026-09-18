package com.example.stay.rest.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean
    OpenAPI stayGatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stay Gateway API")
                        .version("v1")
                        .description("공급사 숙소를 통합 검색하는 API입니다."));
    }
}
