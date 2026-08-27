package com.eduar.promobot.adapter.out.messaging;

import com.eduar.promobot.config.RabbitMQConfig;
import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.model.PromocaoParaEnriquecerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PromocaoPublisher {

    private static final Logger log = LoggerFactory.getLogger(PromocaoPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PromocaoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarParaEnriquecimento(Promocao promocao) {
        PromocaoParaEnriquecerDto mensagem = new PromocaoParaEnriquecerDto(
                promocao.getId(),
                promocao.getProduto().getNome(),
                promocao.getProduto().getCategoria()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_PROMOCAO,
                RabbitMQConfig.ROTEAMENTO_ENRIQUECIMENTO,
                mensagem
        );

        log.info("Promoção {} publicada para enriquecimento", promocao.getId());
    }
}