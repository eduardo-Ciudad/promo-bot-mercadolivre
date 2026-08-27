package com.eduar.promobot.adapter.in.messaging;


import com.eduar.promobot.config.RabbitMQConfig;
import com.eduar.promobot.domain.model.PromocaoParaEnriquecerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PromocaoEnriquecimentoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PromocaoEnriquecimentoConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.FILA_ENRIQUECIMENTO)
    public void receber(PromocaoParaEnriquecerDto mensagem) {
        log.info("Mensagem recebida da fila: id={}, produto={}, categoria={}",
                mensagem.getPromocaoId(),
                mensagem.getProdutoNome(),
                mensagem.getProdutoCategoria());
    }
}
