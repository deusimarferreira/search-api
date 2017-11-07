package com.vivareal.search.api;

import com.vivareal.search.api.configuration.NewRelicTransactionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMappingCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.cloud.netflix.turbine.EnableTurbine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.annotations.ApiIgnore;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static com.google.common.collect.Lists.newArrayList;
import static org.springframework.http.HttpStatus.*;
import static springfox.documentation.builders.PathSelectors.regex;

@SpringBootApplication
@Configuration
@EnableAsync
@ComponentScan(basePackages = {"com.vivareal.search.api.adapter",
        "com.vivareal.search.api.configuration",
        "com.vivareal.search.api.controller",
        "com.vivareal.search.api.service"})
@EnableSwagger2
@EnableHystrix
@EnableHystrixDashboard
@EnableTurbine
public class SearchAPI implements WebMvcConfigurer {

    @Autowired
    private NewRelicTransactionInterceptor requestInterceptor;

    @Bean
    public EndpointHandlerMappingCustomizer mappingCustomizer() {
        return mapping -> mapping.setInterceptors(new Object[] { requestInterceptor });
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
        .apiInfo(apiInfo())
        .select()
        .paths(regex("/v2.*"))
        .build()
        .ignoredParameterTypes(ApiIgnore.class)
        .globalResponseMessage(RequestMethod.GET, newArrayList(statusCode(OK),
                                                               statusCode(NOT_FOUND),
                                                               statusCode(BAD_REQUEST),
                                                               statusCode(INTERNAL_SERVER_ERROR)));
    }

    private ResponseMessage statusCode(HttpStatus status) {
        return new ResponseMessageBuilder().code(status.value()).message(status.getReasonPhrase()).build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
        .title("Search API")
        .termsOfServiceUrl("https://github.com/VivaReal/search-api")
        .version("2.0")
        .build();
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/swagger-ui.html");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestInterceptor);
    }

    public static void main(String[] args) {
        SpringApplication.run(SearchAPI.class, args);
    }
}
