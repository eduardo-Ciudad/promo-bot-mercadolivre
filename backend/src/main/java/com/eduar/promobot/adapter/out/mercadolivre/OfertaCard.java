package com.eduar.promobot.adapter.out.mercadolivre;

public record OfertaCard(
        String titulo,
        String url,
        String imagemUrl,
        String precoAtualLabel,
        String precoAnteriorLabel,
        String descontoLabel
) {
}
