package com.eduar.promobot.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "whatsapp")
public record WhatsAppProperties(

        String accessToken,
        String phoneNumberId,
        String businessAccountId,
        String webhookVerifyToken,
        String apiUrl
) {
}
