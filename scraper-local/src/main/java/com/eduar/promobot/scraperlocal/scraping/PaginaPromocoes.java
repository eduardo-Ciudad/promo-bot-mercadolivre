package com.eduar.promobot.scraperlocal.scraping;

import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;

import java.util.List;
import java.util.Objects;

public record PaginaPromocoes(
        int numero,
        int quantidadeCards,
        List<PromocaoEncontrada> promocoes) {

    public PaginaPromocoes {
        if (numero <= 0) {
            throw new IllegalArgumentException("numero deve ser maior que zero");
        }
        if (quantidadeCards < 0) {
            throw new IllegalArgumentException("quantidadeCards nao pode ser negativa");
        }
        promocoes = List.copyOf(Objects.requireNonNull(promocoes, "promocoes e obrigatoria"));
        if (promocoes.size() > quantidadeCards) {
            throw new IllegalArgumentException("promocoes nao pode exceder quantidadeCards");
        }
    }

    public boolean semCards() {
        return quantidadeCards == 0;
    }
}
