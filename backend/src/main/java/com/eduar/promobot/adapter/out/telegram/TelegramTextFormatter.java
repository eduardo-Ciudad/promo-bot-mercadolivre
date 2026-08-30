package com.eduar.promobot.adapter.out.telegram;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

final class TelegramTextFormatter {

    static final int LIMITE_CAPTION = 1024;
    static final int LIMITE_TEXTO = 4096;

    private TelegramTextFormatter() {
    }

    static String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    static String montarConteudo(String titulo, BigDecimal precoOriginal, BigDecimal precoPromocional,
                                 String texto, String link) {
        StringBuilder conteudo = new StringBuilder();
        if (titulo != null && !titulo.isBlank()) {
            conteudo.append("<b>").append(escaparHtml(titulo)).append("</b>\n\n");
        }
        if (precoOriginal != null && precoPromocional != null) {
            conteudo.append("De: ").append(formatarPreco(precoOriginal))
                    .append(", por: <b>").append(formatarPreco(precoPromocional)).append("</b>\n\n");
        }
        if (texto != null && !texto.isBlank()) {
            conteudo.append(escaparHtml(texto));
        }
        if (link != null && !link.isBlank()) {
            conteudo.append("\n\n").append(escaparHtml(link));
        }
        return conteudo.toString();
    }

    private static String formatarPreco(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return formato.format(valor);
    }

    static String truncar(String conteudo, int limite) {
        if (conteudo.length() <= limite) {
            return conteudo;
        }
        return conteudo.substring(0, limite);
    }
}
