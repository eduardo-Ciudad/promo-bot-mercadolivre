package com.eduar.promobot.application;

import com.eduar.promobot.config.OutboxProperties;
import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.model.DestinoDistribuicao;
import com.eduar.promobot.domain.model.MensagemOutbox;
import com.eduar.promobot.domain.model.Produto;
import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.model.ResultadoEnvio;
import com.eduar.promobot.domain.port.out.DestinoDistribuicaoRepository;
import com.eduar.promobot.domain.port.out.EnviadorDeMensagem;
import com.eduar.promobot.domain.port.out.MensagemOutboxRepository;
import com.eduar.promobot.domain.port.out.PromocaoRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnviarMensagensPendentesUseCaseUrlInvalidaTest {

    @Test
    void deveMarcarUrlS3InvalidaComoFalhaDefinitivaEContinuarLote() {
        MensagemOutbox itemInvalido = mensagem();
        MensagemOutbox itemValido = mensagem();
        MensagemOutboxRepository outbox = mock(MensagemOutboxRepository.class);
        DestinoDistribuicaoRepository destinos = mock(DestinoDistribuicaoRepository.class);
        PromocaoRepository promocoes = mock(PromocaoRepository.class);
        EnviadorRegistry registry = mock(EnviadorRegistry.class);
        EnviadorDeMensagem enviador = mock(EnviadorDeMensagem.class);
        DestinoDistribuicao destino = mock(DestinoDistribuicao.class);
        Promocao promocaoInvalida = promocaoComImagem(
                "https://bucket.s3.amazonaws.com/produto.jpg?AWSAccessKeyId=AKIAEXAMPLE&Expires=1893456000&Signature=abc def%2Bghi%3D");
        Promocao promocaoValida = promocaoComImagem("https://bucket.s3.amazonaws.com/produto-valido.jpg");
        ResultadoEnvio resultado = new ResultadoEnvio("telegram-2", Instant.now());

        when(outbox.buscarPendentesParaEnvio(20, 120)).thenReturn(List.of(itemInvalido, itemValido));
        when(destinos.buscarPorId(any())).thenReturn(Optional.of(destino));
        when(promocoes.buscarPorId(itemInvalido.getPromocaoId())).thenReturn(Optional.of(promocaoInvalida));
        when(promocoes.buscarPorId(itemValido.getPromocaoId())).thenReturn(Optional.of(promocaoValida));
        when(registry.resolver(CanalDistribuicao.TELEGRAM)).thenReturn(Optional.of(enviador));
        when(destino.getExternalId()).thenReturn("123456");
        when(enviador.enviar(any())).thenReturn(resultado);

        EnviarMensagensPendentesUseCase useCase = new EnviarMensagensPendentesUseCase(
                outbox, destinos, promocoes, registry, new OutboxProperties(5000, 20, 3, 10, 120));

        assertThatCode(useCase::processarLote).doesNotThrowAnyException();

        verify(outbox).marcarComoFalha(itemInvalido.getId(),
                "URL de imagem ou link invalido para construcao de URI", null);
        verify(outbox, never()).incrementarTentativa(itemInvalido.getId());
        verify(outbox).incrementarTentativa(itemValido.getId());
        verify(enviador).enviar(any());
        verify(outbox).marcarComoEnviada(itemValido.getId(), resultado.providerMessageId(), resultado.enviadoEm());
    }

    private MensagemOutbox mensagem() {
        return MensagemOutbox.builder()
                .promocaoId(UUID.randomUUID())
                .destinoId(UUID.randomUUID())
                .canal(CanalDistribuicao.TELEGRAM)
                .build();
    }

    private Promocao promocaoComImagem(String imagemUrl) {
        Promocao promocao = mock(Promocao.class);
        Produto produto = mock(Produto.class);
        when(promocao.getProduto()).thenReturn(produto);
        when(produto.getImagemUrl()).thenReturn(imagemUrl);
        when(produto.getNome()).thenReturn("Produto");
        when(promocao.getLinkOriginal()).thenReturn("https://loja.example/produto");
        return promocao;
    }
}
