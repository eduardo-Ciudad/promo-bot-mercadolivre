package com.eduar.promobot.domain.port.out;

import com.eduar.promobot.domain.model.Promocao;

import java.util.List;

public interface BuscadorDePromocoes {

    List<Promocao> buscarPromocoes(CriteriosBusca criterios);

    record CriteriosBusca(int percentualDescontoMinimo, List<String> categorias) {
    }
}
