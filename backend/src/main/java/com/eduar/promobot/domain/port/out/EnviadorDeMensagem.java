package com.eduar.promobot.domain.port.out;

import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.model.MensagemSaida;
import com.eduar.promobot.domain.model.ResultadoEnvio;

public interface EnviadorDeMensagem {

    CanalDistribuicao canal();

    ResultadoEnvio enviar(MensagemSaida mensagem);
}
