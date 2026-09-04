package com.eduar.promobot.scraperlocal.application;

import com.eduar.promobot.scraperlocal.config.ScraperConfig;
import com.eduar.promobot.scraperlocal.ingestao.IngestaoClient;
import com.eduar.promobot.scraperlocal.ingestao.ResultadoIngestao;
import com.eduar.promobot.scraperlocal.model.CriteriosBusca;
import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;
import com.eduar.promobot.scraperlocal.scraping.MercadoLivreScraper;
import com.eduar.promobot.scraperlocal.scraping.PaginaPromocoes;
import com.eduar.promobot.scraperlocal.scraping.SessaoBusca;

import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ScrapingJob {
    private static final Logger LOG = Logger.getLogger(ScrapingJob.class.getName());

    private final Function<CriteriosBusca, SessaoBusca> abrirSessao;
    private final Function<PromocaoEncontrada, ResultadoIngestao> enviarPromocao;
    private final CriteriosBusca criterios;
    private final int alvoMinimoNovas;
    private final int alvoMaximoNovas;
    private final int maxPaginasSegurancaPorCiclo;

    public ScrapingJob(MercadoLivreScraper scraper, IngestaoClient ingestaoClient, ScraperConfig config) {
        this(scraper::iniciarBusca, ingestaoClient::enviar,
                new CriteriosBusca(config.descontoMinimo()),
                config.alvoMinimoNovas(), config.alvoMaximoNovas(),
                config.maxPaginasSegurancaPorCiclo());
    }

    ScrapingJob(Function<CriteriosBusca, SessaoBusca> abrirSessao,
                Function<PromocaoEncontrada, ResultadoIngestao> enviarPromocao,
                CriteriosBusca criterios,
                int alvoMinimoNovas,
                int alvoMaximoNovas,
                int maxPaginasSegurancaPorCiclo) {
        this.abrirSessao = Objects.requireNonNull(abrirSessao, "abrirSessao e obrigatorio");
        this.enviarPromocao = Objects.requireNonNull(enviarPromocao, "enviarPromocao e obrigatorio");
        this.criterios = Objects.requireNonNull(criterios, "criterios e obrigatorio");
        if (alvoMinimoNovas <= 0) throw new IllegalArgumentException("alvoMinimoNovas deve ser positivo");
        if (alvoMaximoNovas < alvoMinimoNovas) {
            throw new IllegalArgumentException("alvoMaximoNovas deve ser maior ou igual a alvoMinimoNovas");
        }
        if (maxPaginasSegurancaPorCiclo <= 0) {
            throw new IllegalArgumentException("maxPaginasSegurancaPorCiclo deve ser positivo");
        }
        this.alvoMinimoNovas = alvoMinimoNovas;
        this.alvoMaximoNovas = alvoMaximoNovas;
        this.maxPaginasSegurancaPorCiclo = maxPaginasSegurancaPorCiclo;
    }

    public ResumoExecucao executar() {
        LOG.info("Iniciando scraping do Mercado Livre");
        ContadoresExecucao contadores = new ContadoresExecucao();
        MotivoParada motivoParada = MotivoParada.LIMITE_PAGINAS;

        try (SessaoBusca sessao = Objects.requireNonNull(
                abrirSessao.apply(criterios), "O scraper retornou uma sessao nula")) {
            busca:
            for (int numeroPagina = 1;
                 numeroPagina <= maxPaginasSegurancaPorCiclo;
                 numeroPagina++) {
                contadores.paginasVisitadas++;
                PaginaPromocoes pagina = Objects.requireNonNull(
                        sessao.buscarPagina(numeroPagina), "O scraper retornou uma pagina nula");
                if (pagina.semCards()) {
                    motivoParada = MotivoParada.FIM_LISTAGEM;
                    break;
                }

                for (PromocaoEncontrada promocao : pagina.promocoes()) {
                    contadores.encontradas++;
                    processarEnvio(promocao, contadores);
                    if (contadores.aceitas >= alvoMaximoNovas) {
                        motivoParada = MotivoParada.ALVO_MAXIMO;
                        break busca;
                    }
                }

                if (contadores.aceitas >= alvoMinimoNovas) {
                    motivoParada = MotivoParada.ALVO_MINIMO;
                    break;
                }
            }
        } catch (RuntimeException e) {
            contadores.falhas++;
            motivoParada = MotivoParada.FALHA_SCRAPING;
            LOG.log(Level.SEVERE, "Falha inesperada durante o scraping", e);
        }

        ResumoExecucao resumo = contadores.resumo(motivoParada);
        LOG.info(() -> "Scraping finalizado: " + resumo);
        return resumo;
    }

    private void processarEnvio(PromocaoEncontrada promocao, ContadoresExecucao contadores) {
        try {
            ResultadoIngestao resultado = Objects.requireNonNull(
                    enviarPromocao.apply(promocao), "O cliente de ingestao retornou resultado nulo");
            switch (resultado) {
                case ACEITA -> contadores.aceitas++;
                case DUPLICATA -> contadores.duplicatas++;
                case PAYLOAD_INVALIDO -> contadores.invalidas++;
                case NAO_AUTORIZADA -> contadores.naoAutorizadas++;
                case ERRO_TRANSITORIO, ERRO_INESPERADO -> contadores.falhas++;
            }
        } catch (RuntimeException e) {
            contadores.falhas++;
            LOG.log(Level.SEVERE, "Falha inesperada ao enviar " + promocao.idExterno(), e);
        }
    }

    public enum MotivoParada {
        ALVO_MAXIMO,
        ALVO_MINIMO,
        FIM_LISTAGEM,
        LIMITE_PAGINAS,
        FALHA_SCRAPING
    }

    public record ResumoExecucao(
            int encontradas,
            int aceitas,
            int duplicatas,
            int invalidas,
            int naoAutorizadas,
            int falhas,
            int paginasVisitadas,
            MotivoParada motivoParada) {
    }

    private static final class ContadoresExecucao {
        private int encontradas;
        private int aceitas;
        private int duplicatas;
        private int invalidas;
        private int naoAutorizadas;
        private int falhas;
        private int paginasVisitadas;

        private ResumoExecucao resumo(MotivoParada motivoParada) {
            return new ResumoExecucao(encontradas, aceitas, duplicatas, invalidas,
                    naoAutorizadas, falhas, paginasVisitadas, motivoParada);
        }
    }
}
