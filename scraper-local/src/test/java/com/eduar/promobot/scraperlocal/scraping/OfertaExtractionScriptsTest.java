package com.eduar.promobot.scraperlocal.scraping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfertaExtractionScriptsTest {

    @Test
    void usaPolyCardMantendoSeletoresInternosDasOfertas() {
        assertTrue(OfertaExtractionScripts.CONTAR_CARDS.contains(".poly-card"));
        assertTrue(OfertaExtractionScripts.EXTRAIR_CARDS.contains(".poly-card"));
        assertFalse(OfertaExtractionScripts.EXTRAIR_CARDS.contains(".ui-search-layout__item"));
        assertTrue(OfertaExtractionScripts.EXTRAIR_CARDS.contains(".poly-component__title"));
        assertTrue(OfertaExtractionScripts.EXTRAIR_CARDS.contains(
                ".poly-price__current .andes-money-amount"));
        assertTrue(OfertaExtractionScripts.EXTRAIR_CARDS.contains(".andes-money-amount--previous"));
        assertTrue(OfertaExtractionScripts.EXTRAIR_CARDS.contains(".poly-price__discount-polylabel"));
        assertTrue(OfertaExtractionScripts.EXTRAIR_CARDS.contains(".poly-component__picture"));
    }
}
