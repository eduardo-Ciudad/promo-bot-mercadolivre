package com.eduar.promobot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(
        long intervaloSchedulerMs,
        int tamanhoLote,
        int maxTentativas,
        long backoffBaseSegundos
) {
    public OutboxProperties {
        if (intervaloSchedulerMs <= 0) {
            intervaloSchedulerMs = 5000;
        }
        if (tamanhoLote <= 0) {
            tamanhoLote = 20;
        }
        if (maxTentativas <= 0) {
            maxTentativas = 5;
        }
        if (backoffBaseSegundos <= 0) {
            backoffBaseSegundos = 10;
        }
    }
}
