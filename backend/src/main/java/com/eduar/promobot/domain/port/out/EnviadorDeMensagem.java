package com.eduar.promobot.domain.port.out;

import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.model.Promocao;

public interface EnviadorDeMensagem {

    void enviar(Promocao promocao);

}