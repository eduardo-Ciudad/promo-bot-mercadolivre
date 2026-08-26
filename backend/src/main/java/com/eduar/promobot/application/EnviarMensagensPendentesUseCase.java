package com.eduar.promobot.application;

import com.eduar.promobot.config.OutboxProperties;
import com.eduar.promobot.domain.exception.DestinatarioIndisponivelException;
import com.eduar.promobot.domain.exception.EnvioFalhouException;
import com.eduar.promobot.domain.exception.EnvioInvalidoException;
import com.eduar.promobot.domain.exception.RateLimitedException;
import com.eduar.promobot.domain.model.DestinoDistribuicao;
import com.eduar.promobot.domain.model.MensagemOutbox;
import com.eduar.promobot.domain.model.MensagemSaida;
import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.model.ResultadoEnvio;
import com.eduar.promobot.domain.port.out.DestinoDistribuicaoRepository;
import com.eduar.promobot.domain.port.out.EnviadorDeMensagem;
import com.eduar.promobot.domain.port.out.MensagemOutboxRepository;
import com.eduar.promobot.domain.port.out.PromocaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EnviarMensagensPendentesUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnviarMensagensPendentesUseCase.class);

    private final MensagemOutboxRepository outboxRepository;
    private final DestinoDistribuicaoRepository destinoRepository;
    private final PromocaoRepository promocaoRepository;
    private final EnviadorRegistry enviadorRegistry;
    private final OutboxProperties properties;

    public EnviarMensagensPendentesUseCase(MensagemOutboxRepository outboxRepository,
                                            DestinoDistribuicaoRepository destinoRepository,
                                            PromocaoRepository promocaoRepository,
                                            EnviadorRegistry enviadorRegistry,
                                            OutboxProperties properties) {
        this.outboxRepository = outboxRepository;
        this.destinoRepository = destinoRepository;
        this.promocaoRepository = promocaoRepository;
        this.enviadorRegistry = enviadorRegistry;
        this.properties = properties;
    }

    public void processarLote() {
        List<MensagemOutbox> pendentes = outboxRepository.buscarPendentesParaEnvio(
                properties.tamanhoLote(), properties.leaseTimeoutSeconds());
        for (MensagemOutbox item : pendentes) {
            processar(item);
        }
    }

    private void processar(MensagemOutbox item) {
        Optional<EnviadorDeMensagem> enviador = enviadorRegistry.resolver(item.getCanal());
        if (enviador.isEmpty()) {
            log.error("Nenhum EnviadorDeMensagem registrado para o canal {}", item.getCanal());
            outboxRepository.marcarComoFalha(item.getId(), "Nenhum enviador registrado para o canal " + item.getCanal(), null);
            return;
        }

        Optional<DestinoDistribuicao> destino = destinoRepository.buscarPorId(item.getDestinoId());
        Optional<Promocao> promocao = promocaoRepository.buscarPorId(item.getPromocaoId());

        if (destino.isEmpty() || promocao.isEmpty()) {
            outboxRepository.marcarComoFalha(item.getId(), "Destino ou promocao nao encontrados", null);
            return;
        }

        MensagemSaida mensagem = construirMensagem(item, destino.get(), promocao.get());

        try {
            outboxRepository.incrementarTentativa(item.getId());
            ResultadoEnvio resultado = enviador.get().enviar(mensagem);
            outboxRepository.marcarComoEnviada(item.getId(), resultado.providerMessageId(), resultado.enviadoEm());
        } catch (RateLimitedException e) {
            int tentativas = item.getTentativas() + 1;
            if (tentativas >= properties.maxTentativas()) {
                outboxRepository.marcarComoFalha(item.getId(),
                        "Limite de tentativas excedido apos rate limit repetido", null);
                return;
            }
            long jitter = ThreadLocalRandom.current().nextLong(0, 5);
            Instant proximaTentativa = Instant.now().plusSeconds(e.getRetryAfterSeconds() + jitter);
            outboxRepository.marcarComoFalha(item.getId(), e.getMessage(), proximaTentativa);
        } catch (DestinatarioIndisponivelException e) {
            destinoRepository.marcarComoBloqueado(destino.get().getId(), Instant.now());
            outboxRepository.marcarComoFalha(item.getId(), e.getMessage(), null);
        } catch (EnvioInvalidoException e) {
            outboxRepository.marcarComoFalha(item.getId(), e.getMessage(), null);
        } catch (EnvioFalhouException e) {
            agendarRetentativaComBackoff(item, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Falha inesperada ao processar mensagem {} do outbox", item.getId(), e);
            agendarRetentativaComBackoff(item, e.getMessage());
        }
    }

    private void agendarRetentativaComBackoff(MensagemOutbox item, String motivo) {
        int tentativas = item.getTentativas() + 1;
        if (tentativas >= properties.maxTentativas()) {
            outboxRepository.marcarComoFalha(item.getId(), motivo, null);
            return;
        }
        long backoffSegundos = properties.backoffBaseSegundos() * (1L << Math.min(tentativas, 10));
        long jitter = ThreadLocalRandom.current().nextLong(0, properties.backoffBaseSegundos());
        Instant proximaTentativa = Instant.now().plusSeconds(backoffSegundos + jitter);
        outboxRepository.marcarComoFalha(item.getId(), motivo, proximaTentativa);
    }

    private MensagemSaida construirMensagem(MensagemOutbox item, DestinoDistribuicao destino, Promocao promocao) {
        URI imagemUrl = toUri(promocao.getProduto().getImagemUrl());
        URI link = toUri(promocao.getLinkAfiliado() != null ? promocao.getLinkAfiliado() : promocao.getLinkOriginal());
        return new MensagemSaida(
                item.getId(),
                item.getCanal(),
                destino.getExternalId(),
                promocao.getDescricaoGerada(),
                imagemUrl,
                link,
                promocao.getProduto().getNome()
        );
    }

    private URI toUri(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return URI.create(valor);
    }
}
