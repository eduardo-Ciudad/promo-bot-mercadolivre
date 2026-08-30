package com.eduar.promobot.scraperlocal.scraping;

final class OfertaExtractionScripts {
    private OfertaExtractionScripts() {
    }

    static final String CONTAR_CARDS =
            "document.querySelectorAll('.poly-card').length";

    static final String EXTRAIR_CARDS = """
            [...document.querySelectorAll('.poly-card')].map(card => {
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
}
