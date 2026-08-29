package com.eduar.promobot.scraperlocal.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PromocaoEncontrada(
        String produtoNome,
        String produtoImagemUrl,
        String produtoCategoria,
        BigDecimal precoOriginal,
        BigDecimal precoPromocional,
        String linkOriginal,
        String idExterno) {

    public PromocaoEncontrada {
        produtoNome = exigirTexto(produtoNome, "produtoNome", 500);
        produtoImagemUrl = validarTextoOpcional(produtoImagemUrl, "produtoImagemUrl", 1000);
        produtoCategoria = validarTextoOpcional(produtoCategoria, "produtoCategoria", 150);
        precoOriginal = exigirPrecoPositivo(precoOriginal, "precoOriginal");
        precoPromocional = exigirPrecoPositivo(precoPromocional, "precoPromocional");
        linkOriginal = exigirTexto(linkOriginal, "linkOriginal", 1000);
        idExterno = exigirTexto(idExterno, "idExterno", 100);
        if (precoPromocional.compareTo(precoOriginal) >= 0) {
            throw new IllegalArgumentException("precoPromocional deve ser menor que precoOriginal");
        }
    }

    public int percentualDesconto() {
        return precoOriginal.subtract(precoPromocional)
                .divide(precoOriginal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).intValue();
    }

    private static BigDecimal exigirPrecoPositivo(BigDecimal valor, String nome) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(nome + " deve ser maior que zero");
        }
        return valor;
    }

    private static String exigirTexto(String valor, String nome, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nome + " e obrigatorio");
        }
        String normalizado = valor.trim();
        validarTamanho(normalizado, nome, tamanhoMaximo);
        return normalizado;
    }

    private static String validarTextoOpcional(String valor, String nome, int tamanhoMaximo) {
        if (valor == null) return null;
        String normalizado = valor.trim();
        if (normalizado.isEmpty()) return null;
        validarTamanho(normalizado, nome, tamanhoMaximo);
        return normalizado;
    }

    private static void validarTamanho(String valor, String nome, int tamanhoMaximo) {
        if (valor.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(nome + " excede o limite de " + tamanhoMaximo + " caracteres");
        }
    }
}

