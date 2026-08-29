package com.eduar.promobot.scraperlocal.application;

import com.eduar.promobot.scraperlocal.ingestao.ResultadoIngestao;
import com.eduar.promobot.scraperlocal.model.CriteriosBusca;
import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrapingJobTest {

    @Test
    void executaFluxoCompletoEConsolidaResultados() {
        CriteriosBusca criterios = new CriteriosBusca(10, List.of("informatica"));
        AtomicReference<CriteriosBusca> criteriosRecebidos = new AtomicReference<>();
        List<PromocaoEncontrada> promocoes = List.of(
                promocao("MLB1"), promocao("MLB2"), promocao("MLB3"),
                promocao("MLB4"), promocao("MLB5"), promocao("MLB6"));
        List<ResultadoIngestao> resultados = List.of(
                ResultadoIngestao.ACEITA,
                ResultadoIngestao.DUPLICATA,
                ResultadoIngestao.PAYLOAD_INVALIDO,
                ResultadoIngestao.NAO_AUTORIZADA,
                ResultadoIngestao.ERRO_TRANSITORIO,
                ResultadoIngestao.ERRO_INESPERADO);

        ScrapingJob job = new ScrapingJob(
                recebidos -> {
                    criteriosRecebidos.set(recebidos);
                    return promocoes;
                },
                promocao -> resultados.get(promocoes.indexOf(promocao)),
                criterios);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(criterios, criteriosRecebidos.get());
        assertEquals(new ScrapingJob.ResumoExecucao(6, 1, 1, 1, 1, 2), resumo);
    }

    @Test
    void contabilizaExcecaoDeUmEnvioEContinuaOsDemais() {
        List<PromocaoEncontrada> promocoes = List.of(promocao("MLB1"), promocao("MLB2"));
        ScrapingJob job = new ScrapingJob(
                criterios -> promocoes,
                promocao -> {
                    if (promocao.idExterno().equals("MLB1")) {
                        throw new IllegalStateException("falha simulada");
                    }
                    return ResultadoIngestao.ACEITA;
                },
                new CriteriosBusca(10, List.of()));

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(new ScrapingJob.ResumoExecucao(2, 1, 0, 0, 0, 1), resumo);
    }

    private PromocaoEncontrada promocao(String idExterno) {
        return new PromocaoEncontrada(
                "Notebook", null, "informatica",
                new BigDecimal("100.00"), new BigDecimal("80.00"),
                "https://produto.mercadolivre.com.br/" + idExterno, idExterno);
    }
}
