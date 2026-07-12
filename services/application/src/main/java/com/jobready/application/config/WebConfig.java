package com.jobready.application.config;

import com.jobready.application.generated.modelDto.ApplicationStage;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Binds the `stage` query param by its lowercase wire value (`applied`, `follow_up`, …) —
     * Spring's default enum converter only accepts the Java constant name (`APPLIED`).
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, ApplicationStage.class, ApplicationStage::fromValue);
    }
}
