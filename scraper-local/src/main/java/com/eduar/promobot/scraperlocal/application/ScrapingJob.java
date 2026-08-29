package com.eduar.promobot.scraperlocal.application;

import com.eduar.promobot.scraperlocal.config.ScraperConfig;
import com.eduar.promobot.scraperlocal.ingestao.IngestaoClient;
import com.eduar.promobot.scraperlocal.ingestao.ResultadoIngestao;
import com.eduar.promobot.scraperlocal.model.CriteriosBusca;
import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;
import com.eduar.promobot.scraperlocal.scraping.MercadoLivreScraper;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ScrapingJob {
    private static final Logger LOG = Logger.getLogger(ScrapingJob.class.getName());

    private final Function<CriteriosBusca, List<PromocaoEncontrada>> buscarPromocoes;
    private final Function<PromocaoEncontrada, ResultadoIngestao> enviarPromocao;
    private final CriteriosBusca criterios;

    public ScrapingJob(MercadoLivreScraper scraper, IngestaoClient ingestaoClient, ScraperConfig config) {
        this(scraper::buscarPromocoes, ingestaoClient::enviar,
                new CriteriosBusca(config.descontoMinimo(), config.categorias()));
    }

    ScrapingJob(Function<CriteriosBusca, List<PromocaoEncontrada>> buscarPromocoes,
                Function<PromocaoEncontrada, ResultadoIngestao> enviarPromocao,
                CriteriosBusca criterios) {
        this.buscarPromocoes = Objects.requireNonNull(buscarPromocoes, "buscarPromocoes e obrigatorio");
        this.enviarPromocao = Objects.requireNonNull(enviarPromocao, "enviarPromocao e obrigatorio");
        this.criterios = Objects.requireNonNull(criterios, "criterios e obrigatorio");
    }

    public ResumoExecucao executar() {
        LOG.info("Iniciando scraping do Mercado Livre");
        List<PromocaoEncontrada> encontradas = Objects.requireNonNull(
                buscarPromocoes.apply(criterios), "O scraper retornou uma lista nula");

        int aceitas = 0;
        int duplicatas = 0;
        int invalidas = 0;
        int naoAutorizadas = 0;
        int falhas = 0;

        for (PromocaoEncontrada promocao : encontradas) {
            try {
                ResultadoIngestao resultado = Objects.requireNonNull(
                        enviarPromocao.apply(promocao), "O cliente de ingestao retornou resultado nulo");
                switch (resultado) {
                    case ACEITA -> aceitas++;
                    case DUPLICATA -> duplicatas++;
                    case PAYLOAD_INVALIDO -> invalidas++;
                    case NAO_AUTORIZADA -> naoAutorizadas++;
                    case ERRO_TRANSITORIO, ERRO_INESPERADO -> falhas++;
                }
            } catch (RuntimeException e) {
                falhas++;
                LOG.log(Level.SEVERE, "Falha inesperada ao enviar " + promocao.idExterno(), e);
            }
        }

        ResumoExecucao resumo = new ResumoExecucao(
                encontradas.size(), aceitas, duplicatas, invalidas, naoAutorizadas, falhas);
        LOG.info(() -> "Scraping finalizado: " + resumo);
        return resumo;
    }

    public record ResumoExecucao(
            int encontradas,
            int aceitas,
            int duplicatas,
            int invalidas,
            int naoAutorizadas,
            int falhas) {
    }
}

