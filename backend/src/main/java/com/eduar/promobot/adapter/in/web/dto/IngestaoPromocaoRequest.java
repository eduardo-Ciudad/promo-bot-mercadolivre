package com.eduar.promobot.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record IngestaoPromocaoRequest(
        @NotBlank String produtoNome,
        String produtoImagemUrl,
        String produtoCategoria,
        @NotNull @Positive BigDecimal precoOriginal,
        @NotNull @Positive BigDecimal precoPromocional,
        @NotBlank String linkOriginal,
        @NotBlank String idExterno
) {
}
