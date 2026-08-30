package com.eduar.promobot.domain.model;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

public record MensagemSaida(
        UUID mensagemId,
        CanalDistribuicao canal,
        String destinoExterno,
        String texto,
        URI imagemUrl,
        URI link,
        String titulo,
        BigDecimal precoOriginal,
        BigDecimal precoPromocional
) {
}
