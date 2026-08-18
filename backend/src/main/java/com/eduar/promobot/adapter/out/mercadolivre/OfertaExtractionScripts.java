package com.eduar.promobot.adapter.out.mercadolivre;

final class OfertaExtractionScripts {
    private OfertaExtractionScripts() {
    }

    static final String CONTAR_CARDS =
            "document.querySelectorAll('.ui-search-layout__item').length";

    static final String EXTRAIR_CARDS = """
            [...document.querySelectorAll('.ui-search-layout__item')].map(card => {
                const current = card.querySelector('.poly-price__current .andes-money-amount');
                const previous = card.querySelector('.andes-money-amount--previous');

                return {
                    titulo: card.querySelector('.poly-component__title')?.innerText?.trim() ?? null,
                    url: card.querySelector('a')?.href ?? null,
                    imagemUrl: card.querySelector('.poly-component__picture')?.src ?? null,
                    precoAtualLabel: current?.getAttribute('aria-label') ?? null,
                    precoAnteriorLabel: previous?.getAttribute('aria-label') ?? null,
                    descontoLabel: card.querySelector('.poly-price__discount-polylabel')?.innerText?.trim() ?? null
                };
            })
            """;

    static final String SCROLL_PARA_BAIXO =
            "window.scrollTo(0, document.body.scrollHeight)";
}
