package com.eduar.promobot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties (
        String apiKey,
        String apiUrl
){

    @Override
    public String toString() {
        return "GeminiProperties[apiKey=***, apiUrl=%s]".formatted(apiUrl);
    }
}
