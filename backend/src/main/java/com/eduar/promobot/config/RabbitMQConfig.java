package com.eduar.promobot.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String FILA_ENRIQUECIMENTO = "fila.enriquecimento.promocao";
    public static final String EXCHANGE_PROMOCAO = "exchange.promocao";
    public static final String ROTEAMENTO_ENRIQUECIMENTO = "promocao.enriquecer";

    @Bean
    public Queue filaEnriquecimento() {
        return new Queue(FILA_ENRIQUECIMENTO, true);
    }

    @Bean
    public DirectExchange exchangePromocao() {
        return new DirectExchange(EXCHANGE_PROMOCAO);
    }

    @Bean
    public Binding bindingEnriquecimento(Queue filaEnriquecimento, DirectExchange exchangePromocao) {
        return BindingBuilder
                .bind(filaEnriquecimento)
                .to(exchangePromocao)
                .with(ROTEAMENTO_ENRIQUECIMENTO);
    }
}
