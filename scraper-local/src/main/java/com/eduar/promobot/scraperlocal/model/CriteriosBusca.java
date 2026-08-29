package com.eduar.promobot.scraperlocal.model;

import java.util.List;
import java.util.Objects;

public record CriteriosBusca(int percentualDescontoMinimo, List<String> categorias) {
    public CriteriosBusca {
        if (percentualDescontoMinimo < 0 || percentualDescontoMinimo > 100) {
            throw new IllegalArgumentException("percentualDescontoMinimo deve estar entre 0 e 100");
        }
        if (categorias == null) {
            categorias = List.of();
        } else {
            categorias = categorias.stream().filter(Objects::nonNull).map(String::trim)
                    .filter(categoria -> !categoria.isEmpty()).distinct().toList();
        }
    }
}
