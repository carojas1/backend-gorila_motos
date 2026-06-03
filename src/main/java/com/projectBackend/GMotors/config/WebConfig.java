package com.projectBackend.GMotors.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/images/usuarios/**")
                .addResourceLocations(
                    "file:C:/Users/USUARIO/Desktop/prototipoSpring/gmotors/uploads/usuarios/"
                )
                .setCachePeriod(0);
        
        
        registry.addResourceHandler("/images/motos/**")
                .addResourceLocations(
                    "file:C:/Users/USUARIO/Desktop/prototipoSpring/gmotors/uploads/motos/"
                )
                .setCachePeriod(0);
        
        registry.addResourceHandler("/images/productos/**")
        .addResourceLocations(
            "file:C:/Users/USUARIO/Desktop/prototipoSpring/gmotors/uploads/productos/"
        )
        .setCachePeriod(0);
    }
}
