package com.eduar.promobot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(
        String botToken,
        String apiBaseUrl,
        String webhookSecretToken
) {
    public TelegramProperties {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "https://api.telegram.org";
        }
    }

    @Override
    public String toString() {
        return "TelegramProperties[botToken=***, apiBaseUrl=%s, webhookSecretToken=***]".formatted(apiBaseUrl);
    }
}
