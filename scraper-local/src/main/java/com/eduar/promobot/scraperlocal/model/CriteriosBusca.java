package com.eduar.promobot.scraperlocal.model;

public record CriteriosBusca(int percentualDescontoMinimo) {
    public CriteriosBusca {
        if (percentualDescontoMinimo < 0 || percentualDescontoMinimo > 100) {
            throw new IllegalArgumentException("percentualDescontoMinimo deve estar entre 0 e 100");
        }
    }
}
