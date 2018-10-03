package com.nbc.app.config;


import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
@Configuration
@EnableSwagger2
public class SwaggerConfig {
	
	private static final String SWAGGER_API_VERSION="1.0";
	private static final String TITLE="ALFRESCO SERVICE";
	private static final String DESCRIPTION="Uploading a file to Alfresco DMS";

	@Bean
    public Docket swaggerSpringfoxDocket() {
		ApiInfo apiInfo = new ApiInfoBuilder().title(TITLE)
				.description(DESCRIPTION)
				.version(SWAGGER_API_VERSION)
				.build();
       
		 List<ResponseMessage> customResponses= new ArrayList<>();
	       
	        customResponses
			.add(new ResponseMessageBuilder().code(400).message("Invalid parameters passed").build());
	        customResponses.add(new ResponseMessageBuilder().code(500).message("Application Error").build());
	        customResponses.add(new ResponseMessageBuilder().code(401).message("You are not authorized to view the resource").build());
	        customResponses.add(new ResponseMessageBuilder().code(403).message("Accessing the resource you were trying to reach is forbidden").build());
	        customResponses.add(new ResponseMessageBuilder().code(404).message("The resource you were trying to reach is not found").build());
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .useDefaultResponseMessages(true)
                .globalResponseMessage(RequestMethod.POST, 
                		customResponses)
                .globalResponseMessage(RequestMethod.GET, 
                		customResponses)
                .globalResponseMessage(RequestMethod.DELETE, 
                		customResponses)
                .genericModelSubstitutes(ResponseEntity.class)
                .forCodeGeneration(true)
                .select().apis(RequestHandlerSelectors.basePackage("com.nbc.app.controller"))
                .paths(PathSelectors.any())
                .build();
    }
}
