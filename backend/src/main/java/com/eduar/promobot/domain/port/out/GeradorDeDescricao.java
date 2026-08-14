package com.eduar.promobot.domain.port.out;

import com.eduar.promobot.domain.model.Promocao;

public interface GeradorDeDescricao {

    String gerarDescricao(Promocao promocao);
}
