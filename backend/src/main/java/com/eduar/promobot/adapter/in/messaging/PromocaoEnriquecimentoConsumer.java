package com.eduar.promobot.adapter.in.messaging;


import com.eduar.promobot.adapter.out.persistence.DestinoDistribuicaoJpaRepository;
import com.eduar.promobot.adapter.out.persistence.MensagemOutboxJpaRepository;
import com.eduar.promobot.config.RabbitMQConfig;
import com.eduar.promobot.domain.model.*;
import com.eduar.promobot.domain.port.out.GeradorDeDescricao;
import com.eduar.promobot.domain.port.out.PromocaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PromocaoEnriquecimentoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PromocaoEnriquecimentoConsumer.class);

    private final PromocaoRepository promocaoRepository;
    private final GeradorDeDescricao geradorDeDescricao;
    private final DestinoDistribuicaoJpaRepository destinoDistribuicaoJpaRepository;
    private final MensagemOutboxJpaRepository mensagemOutboxJpaRepository;
    private final String destinoPadraoExternalId;

    public PromocaoEnriquecimentoConsumer(PromocaoRepository promocaoRepository,
                                          GeradorDeDescricao geradorDeDescricao,
                                          DestinoDistribuicaoJpaRepository destinoDistribuicaoJpaRepository,
                                          MensagemOutboxJpaRepository mensagemOutboxJpaRepository,
                                          @Value("${promobot.destino.padrao.external-id}") String destinoPadraoExternalId) {
        this.promocaoRepository = promocaoRepository;
        this.geradorDeDescricao = geradorDeDescricao;
        this.destinoDistribuicaoJpaRepository = destinoDistribuicaoJpaRepository;
        this.mensagemOutboxJpaRepository = mensagemOutboxJpaRepository;
        this.destinoPadraoExternalId = destinoPadraoExternalId;
    }



    @RabbitListener(queues = RabbitMQConfig.FILA_ENRIQUECIMENTO)
    public void receber(PromocaoParaEnriquecerDto mensagem) {
        log.info("Mensagem recebida da fila: id={}, produto={}", mensagem.getPromocaoId(), mensagem.getProdutoNome());

        Optional<Promocao> promocaoOpt = promocaoRepository.buscarPorId(mensagem.getPromocaoId());
        if (promocaoOpt.isEmpty()) {
            log.warn("Promoção {} não encontrada no banco. Ignorando mensagem.", mensagem.getPromocaoId());
            return;
        }
        Promocao promocao = promocaoOpt.get();

        String descricao = geradorDeDescricao.gerarDescricao(promocao);
        promocao.enriquecerComDescricao(descricao);
        promocaoRepository.salvar(promocao);
        log.info("Promoção {} enriquecida e salva com status {}", promocao.getId(), promocao.getStatus());

        Optional<DestinoDistribuicao> destinoOpt = destinoDistribuicaoJpaRepository
                .findByExternalIdAndCanal(destinoPadraoExternalId, CanalDistribuicao.TELEGRAM);

        if (destinoOpt.isEmpty()) {
            log.warn("Nenhum destino ativo encontrado para o canal Telegram. Mensagem não será enviada.");
            return;
        }
        DestinoDistribuicao destino = destinoOpt.get();

        MensagemOutbox outbox = MensagemOutbox.builder()
                .promocaoId(promocao.getId())
                .destinoId(destino.getId())
                .canal(CanalDistribuicao.TELEGRAM)
                .build();
        mensagemOutboxJpaRepository.save(outbox);
        log.info("Mensagem criada no outbox para a promoção {}", promocao.getId());
    }
}
