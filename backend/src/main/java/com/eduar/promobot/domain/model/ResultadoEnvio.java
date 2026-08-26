package com.eduar.promobot.domain.model;

import java.time.Instant;

public record ResultadoEnvio(
        String providerMessageId,
        Instant enviadoEm
) {
}
