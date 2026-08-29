package com.eduar.promobot.scraperlocal.ingestao;

import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;

import java.math.BigDecimal;
import java.util.Objects;

public final class JsonSerializer {
    private JsonSerializer() {
    }

    public static String serializar(PromocaoEncontrada promocao) {
        Objects.requireNonNull(promocao, "promocao e obrigatoria");

        return "{" +
                campoTexto("produtoNome", promocao.produtoNome()) + "," +
                campoTexto("produtoImagemUrl", promocao.produtoImagemUrl()) + "," +
                campoTexto("produtoCategoria", promocao.produtoCategoria()) + "," +
                campoNumero("precoOriginal", promocao.precoOriginal()) + "," +
                campoNumero("precoPromocional", promocao.precoPromocional()) + "," +
                campoTexto("linkOriginal", promocao.linkOriginal()) + "," +
                campoTexto("idExterno", promocao.idExterno()) +
                "}";
    }

    private static String campoTexto(String nome, String valor) {
        return aspas(nome) + ":" + (valor == null ? "null" : aspas(valor));
    }

    private static String campoNumero(String nome, BigDecimal valor) {
        return aspas(nome) + ":" + valor.toPlainString();
    }

    private static String aspas(String valor) {
        return "\"" + escapar(valor) + "\"";
    }

    private static String escapar(String valor) {
        StringBuilder resultado = new StringBuilder(valor.length() + 16);
        for (int i = 0; i < valor.length(); i++) {
            char caractere = valor.charAt(i);
            switch (caractere) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\b' -> resultado.append("\\b");
                case '\f' -> resultado.append("\\f");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                default -> {
                    if (caractere < 0x20) {
                        resultado.append(String.format("\\u%04x", (int) caractere));
                    } else {
                        resultado.append(caractere);
                    }
                }
            }
        }
        return resultado.toString();
    }
}

