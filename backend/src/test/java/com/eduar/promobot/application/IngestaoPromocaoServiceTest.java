package com.eduar.promobot.application;

import com.eduar.promobot.adapter.out.messaging.PromocaoPublisher;
import com.eduar.promobot.domain.model.Produto;
import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.port.out.PromocaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngestaoPromocaoServiceTest {

    private PromocaoRepository promocaoRepository;
    private PromocaoPublisher promocaoPublisher;
    private IngestaoPromocaoService service;

    @BeforeEach
    void setUp() {
        promocaoRepository = mock(PromocaoRepository.class);
        promocaoPublisher = mock(PromocaoPublisher.class);
        service = new IngestaoPromocaoService(promocaoRepository, promocaoPublisher);
    }

    @Test
    void deveSalvarPublicarERetornarAceitaQuandoPromocaoForNova() {
        Promocao promocao = novaPromocao();
        when(promocaoRepository.existePorIdExterno(promocao.getIdExterno())).thenReturn(false);

        ResultadoIngestao resultado = service.ingerir(promocao);

        assertEquals(ResultadoIngestao.ACEITA, resultado);
        verify(promocaoRepository).salvar(promocao);
        verify(promocaoPublisher).publicarParaEnriquecimento(promocao);
    }

    @Test
    void deveIgnorarSemSalvarNemPublicarQuandoPromocaoForDuplicada() {
        Promocao promocao = novaPromocao();
        when(promocaoRepository.existePorIdExterno(promocao.getIdExterno())).thenReturn(true);

        ResultadoIngestao resultado = service.ingerir(promocao);

        assertEquals(ResultadoIngestao.IGNORADA_DUPLICATA, resultado);
        verify(promocaoRepository, never()).salvar(promocao);
        verify(promocaoPublisher, never()).publicarParaEnriquecimento(promocao);
    }

    private Promocao novaPromocao() {
        return Promocao.builder()
                .produto(new Produto("Produto teste", "https://exemplo.com/imagem.jpg", "informatica"))
                .precoOriginal(new BigDecimal("100.00"))
                .precoPromocional(new BigDecimal("80.00"))
                .linkOriginal("https://exemplo.com/produto")
                .idExterno("MLB123456")
                .build();
    }
}
