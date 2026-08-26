package com.eduar.promobot.application;

import com.eduar.promobot.config.OutboxProperties;
import com.eduar.promobot.domain.exception.RateLimitedException;
import com.eduar.promobot.domain.model.*;
import com.eduar.promobot.domain.port.out.DestinoDistribuicaoRepository;
import com.eduar.promobot.domain.port.out.EnviadorDeMensagem;
import com.eduar.promobot.domain.port.out.MensagemOutboxRepository;
import com.eduar.promobot.domain.port.out.PromocaoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EnviarMensagensPendentesUseCaseRateLimitTest {

    @Test
    void deveEncerrarEmFalhaAoAtingirLimiteDeTentativasApos429Repetido() {
        UUID promocaoId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();
        MensagemOutbox primeiraTentativa = mensagem(promocaoId, destinoId, 0);
        MensagemOutbox segundaTentativa = mensagem(promocaoId, destinoId, 1);
        MensagemOutbox terceiraTentativa = mensagem(promocaoId, destinoId, 2);
        MensagemOutboxRepository outbox = mock(MensagemOutboxRepository.class);
        DestinoDistribuicaoRepository destinos = mock(DestinoDistribuicaoRepository.class);
        PromocaoRepository promocoes = mock(PromocaoRepository.class);
        EnviadorRegistry registry = mock(EnviadorRegistry.class);
        EnviadorDeMensagem enviador = mock(EnviadorDeMensagem.class);
        DestinoDistribuicao destino = mock(DestinoDistribuicao.class);
        Promocao promocao = mock(Promocao.class);
        Produto produto = mock(Produto.class);

        when(outbox.buscarPendentesParaEnvio(1, 120))
                .thenReturn(List.of(primeiraTentativa), List.of(segundaTentativa), List.of(terceiraTentativa));
        when(destinos.buscarPorId(destinoId)).thenReturn(Optional.of(destino));
        when(promocoes.buscarPorId(promocaoId)).thenReturn(Optional.of(promocao));
        when(registry.resolver(CanalDistribuicao.TELEGRAM)).thenReturn(Optional.of(enviador));
        when(destino.getExternalId()).thenReturn("123456");
        when(promocao.getProduto()).thenReturn(produto);
        when(produto.getNome()).thenReturn("Produto");
        when(enviador.enviar(any())).thenThrow(new RateLimitedException(30));

        EnviarMensagensPendentesUseCase useCase = new EnviarMensagensPendentesUseCase(
                outbox, destinos, promocoes, registry, new OutboxProperties(5000, 1, 3, 10, 120));

        useCase.processarLote();
        useCase.processarLote();
        useCase.processarLote();

        ArgumentCaptor<String> motivos = ArgumentCaptor.forClass(String.class);
        verify(outbox, times(3)).marcarComoFalha(any(UUID.class), motivos.capture(), nullable(java.time.Instant.class));
        assertThat(motivos.getAllValues().get(2))
                .isEqualTo("Limite de tentativas excedido apos rate limit repetido");
        verify(outbox).marcarComoFalha(terceiraTentativa.getId(),
                "Limite de tentativas excedido apos rate limit repetido", null);
        verify(outbox, times(3)).incrementarTentativa(any(UUID.class));
    }

    private MensagemOutbox mensagem(UUID promocaoId, UUID destinoId, int tentativas) {
        MensagemOutbox mensagem = MensagemOutbox.builder()
                .promocaoId(promocaoId)
                .destinoId(destinoId)
                .canal(CanalDistribuicao.TELEGRAM)
                .build();
        for (int i = 0; i < tentativas; i++) {
            mensagem.incrementarTentativa();
        }
        return mensagem;
    }
}
