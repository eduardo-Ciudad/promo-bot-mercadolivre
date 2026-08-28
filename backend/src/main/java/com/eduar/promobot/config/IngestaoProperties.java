package com.eduar.promobot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingestao")
public record IngestaoProperties(String apiKey) {

    @Override
    public String toString() {
        return "IngestaoProperties[apiKey=***]";
    }
}
