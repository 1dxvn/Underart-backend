package com.underart.infrastructure.web;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "local")
public class LocalMediaWebConfig implements WebMvcConfigurer {

    private final Path mediaDirectory;

    public LocalMediaWebConfig(@Value("${storage.local.directory}") String directory) {
        mediaDirectory = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (String folder : new String[]{"artworks", "avatars"}) {
            String location = mediaDirectory.resolve(folder).toUri().toString();
            registry.addResourceHandler("/media/" + folder + "/**")
                    .addResourceLocations(location.endsWith("/") ? location : location + "/");
        }
    }
}
