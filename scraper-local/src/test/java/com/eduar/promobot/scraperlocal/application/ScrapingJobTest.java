package com.eduar.promobot.scraperlocal.application;

import com.eduar.promobot.scraperlocal.ingestao.ResultadoIngestao;
import com.eduar.promobot.scraperlocal.model.CriteriosBusca;
import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;
import com.eduar.promobot.scraperlocal.scraping.PaginaPromocoes;
import com.eduar.promobot.scraperlocal.scraping.SessaoBusca;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.eduar.promobot.scraperlocal.application.ScrapingJob.MotivoParada.ALVO_MAXIMO;
import static com.eduar.promobot.scraperlocal.application.ScrapingJob.MotivoParada.ALVO_MINIMO;
import static com.eduar.promobot.scraperlocal.application.ScrapingJob.MotivoParada.FALHA_SCRAPING;
import static com.eduar.promobot.scraperlocal.application.ScrapingJob.MotivoParada.FIM_LISTAGEM;
import static com.eduar.promobot.scraperlocal.application.ScrapingJob.MotivoParada.LIMITE_PAGINAS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrapingJobTest {

    @Test
    void paraNoMeioDaPaginaAoAtingirAlvoMaximo() {
        List<PromocaoEncontrada> promocoes = promocoes(10);
        List<String> idsEnviados = new ArrayList<>();
        List<Integer> paginasVisitadas = new ArrayList<>();
        ScrapingJob job = job(
                sessao(Map.of(1, pagina(1, 10, promocoes)), paginasVisitadas, -1, new AtomicBoolean()),
                promocao -> {
                    idsEnviados.add(promocao.idExterno());
                    return ResultadoIngestao.ACEITA;
                }, 5, 8, 12);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(promocoes.subList(0, 8).stream().map(PromocaoEncontrada::idExterno).toList(), idsEnviados);
        assertEquals(List.of(1), paginasVisitadas);
        assertEquals(new ScrapingJob.ResumoExecucao(8, 8, 0, 0, 0, 0, 1, ALVO_MAXIMO), resumo);
    }

    @Test
    void verificaAlvoMinimoSomenteAoFinalDaPagina() {
        List<PromocaoEncontrada> promocoes = promocoes(6);
        List<Integer> paginasVisitadas = new ArrayList<>();
        ScrapingJob job = job(
                sessao(Map.of(1, pagina(1, 6, promocoes)), paginasVisitadas, -1, new AtomicBoolean()),
                promocao -> promocao.idExterno().equals("MLB6")
                        ? ResultadoIngestao.DUPLICATA
                        : ResultadoIngestao.ACEITA,
                5, 8, 12);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(List.of(1), paginasVisitadas);
        assertEquals(new ScrapingJob.ResumoExecucao(6, 5, 1, 0, 0, 0, 1, ALVO_MINIMO), resumo);
    }

    @Test
    void continuaBuscandoEnquantoMinimoNaoFoiAtingido() {
        List<Integer> paginasVisitadas = new ArrayList<>();
        ScrapingJob job = job(
                sessao(Map.of(
                        1, pagina(1, 1, List.of(promocao("MLB1"))),
                        2, pagina(2, 1, List.of(promocao("MLB2"))),
                        3, pagina(3, 0, List.of())), paginasVisitadas, -1, new AtomicBoolean()),
                promocao -> ResultadoIngestao.ACEITA, 5, 8, 12);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(List.of(1, 2, 3), paginasVisitadas);
        assertEquals(new ScrapingJob.ResumoExecucao(2, 2, 0, 0, 0, 0, 3, FIM_LISTAGEM), resumo);
    }

    @Test
    void encerraAoEncontrarPaginaSemCards() {
        List<Integer> paginasVisitadas = new ArrayList<>();
        ScrapingJob job = job(
                sessao(Map.of(1, pagina(1, 0, List.of())), paginasVisitadas, -1, new AtomicBoolean()),
                promocao -> ResultadoIngestao.ACEITA, 5, 8, 12);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(List.of(1), paginasVisitadas);
        assertEquals(new ScrapingJob.ResumoExecucao(0, 0, 0, 0, 0, 0, 1, FIM_LISTAGEM), resumo);
    }

    @Test
    void paginaComCardsSemPromocoesValidasNaoEncerraAListagem() {
        List<Integer> paginasVisitadas = new ArrayList<>();
        ScrapingJob job = job(
                sessao(Map.of(
                        1, pagina(1, 3, List.of()),
                        2, pagina(2, 5, promocoes(5))), paginasVisitadas, -1, new AtomicBoolean()),
                promocao -> ResultadoIngestao.ACEITA, 5, 8, 12);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(List.of(1, 2), paginasVisitadas);
        assertEquals(new ScrapingJob.ResumoExecucao(5, 5, 0, 0, 0, 0, 2, ALVO_MINIMO), resumo);
    }

    @Test
    void respeitaDeduplicacaoDaSessaoEContinuaQuandoPaginaSoTemIdsRepetidos() {
        List<Integer> paginasVisitadas = new ArrayList<>();
        ScrapingJob job = job(
                sessao(Map.of(
                        1, pagina(1, 1, List.of(promocao("MLB1"))),
                        2, pagina(2, 2, List.of()),
                        3, pagina(3, 0, List.of())), paginasVisitadas, -1, new AtomicBoolean()),
                promocao -> ResultadoIngestao.DUPLICATA, 5, 8, 12);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(List.of(1, 2, 3), paginasVisitadas);
        assertEquals(new ScrapingJob.ResumoExecucao(1, 0, 1, 0, 0, 0, 3, FIM_LISTAGEM), resumo);
    }

    @Test
    void falhaDeScrapingNaoETratadaComoPaginaVaziaEPreservaResumoParcial() {
        List<Integer> paginasVisitadas = new ArrayList<>();
        AtomicBoolean sessaoFechada = new AtomicBoolean();
        ScrapingJob job = job(
                sessao(Map.of(1, pagina(1, 1, List.of(promocao("MLB1")))),
                        paginasVisitadas, 2, sessaoFechada),
                promocao -> ResultadoIngestao.DUPLICATA, 5, 8, 12);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(List.of(1, 2), paginasVisitadas);
        assertTrue(sessaoFechada.get());
        assertEquals(new ScrapingJob.ResumoExecucao(1, 0, 1, 0, 0, 1, 2, FALHA_SCRAPING), resumo);
    }

    @Test
    void contabilizaFalhaIsoladaDeEnvioEContinuaOsDemais() {
        List<Integer> paginasVisitadas = new ArrayList<>();
        ScrapingJob job = job(
                sessao(Map.of(
                        1, pagina(1, 2, List.of(promocao("MLB1"), promocao("MLB2"))),
                        2, pagina(2, 0, List.of())), paginasVisitadas, -1, new AtomicBoolean()),
                promocao -> {
                    if (promocao.idExterno().equals("MLB1")) throw new IllegalStateException("falha simulada");
                    return ResultadoIngestao.ACEITA;
                }, 5, 8, 12);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(new ScrapingJob.ResumoExecucao(2, 1, 0, 0, 0, 1, 2, FIM_LISTAGEM), resumo);
    }

    @Test
    void encerraNoLimiteDeSegurancaQuandoNenhumaMetaEAlcancada() {
        List<Integer> paginasVisitadas = new ArrayList<>();
        ScrapingJob job = job(
                sessao(Map.of(
                        1, pagina(1, 1, List.of(promocao("MLB1"))),
                        2, pagina(2, 1, List.of(promocao("MLB2")))),
                        paginasVisitadas, -1, new AtomicBoolean()),
                promocao -> ResultadoIngestao.DUPLICATA, 5, 8, 2);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(List.of(1, 2), paginasVisitadas);
        assertEquals(new ScrapingJob.ResumoExecucao(2, 0, 2, 0, 0, 0, 2, LIMITE_PAGINAS), resumo);
    }

    @Test
    void consolidaTodosOsResultadosDeIngestaoEPreservaCriterios() {
        CriteriosBusca criterios = new CriteriosBusca(10);
        AtomicReference<CriteriosBusca> criteriosRecebidos = new AtomicReference<>();
        List<PromocaoEncontrada> promocoes = promocoes(6);
        List<ResultadoIngestao> resultados = List.of(
                ResultadoIngestao.ACEITA,
                ResultadoIngestao.DUPLICATA,
                ResultadoIngestao.PAYLOAD_INVALIDO,
                ResultadoIngestao.NAO_AUTORIZADA,
                ResultadoIngestao.ERRO_TRANSITORIO,
                ResultadoIngestao.ERRO_INESPERADO);
        SessaoBusca sessao = sessao(Map.of(
                1, pagina(1, 6, promocoes),
                2, pagina(2, 0, List.of())), new ArrayList<>(), -1, new AtomicBoolean());
        ScrapingJob job = new ScrapingJob(
                recebidos -> {
                    criteriosRecebidos.set(recebidos);
                    return sessao;
                }, promocao -> resultados.get(promocoes.indexOf(promocao)), criterios, 5, 8, 12);

        ScrapingJob.ResumoExecucao resumo = job.executar();

        assertEquals(criterios, criteriosRecebidos.get());
        assertEquals(new ScrapingJob.ResumoExecucao(6, 1, 1, 1, 1, 2, 2, FIM_LISTAGEM), resumo);
    }

    private ScrapingJob job(SessaoBusca sessao,
                            Function<PromocaoEncontrada, ResultadoIngestao> enviar,
                            int alvoMinimo,
                            int alvoMaximo,
                            int maxPaginas) {
        return new ScrapingJob(criterios -> sessao, enviar, new CriteriosBusca(10),
                alvoMinimo, alvoMaximo, maxPaginas);
    }

    private SessaoBusca sessao(Map<Integer, PaginaPromocoes> paginas,
                               List<Integer> visitadas,
                               int paginaComFalha,
                               AtomicBoolean fechada) {
        return new SessaoBusca() {
            @Override
            public PaginaPromocoes buscarPagina(int numeroPagina) {
                visitadas.add(numeroPagina);
                if (numeroPagina == paginaComFalha) throw new IllegalStateException("falha simulada");
                return paginas.get(numeroPagina);
            }

            @Override
            public void close() {
                fechada.set(true);
            }
        };
    }

    private PaginaPromocoes pagina(int numero, int quantidadeCards, List<PromocaoEncontrada> promocoes) {
        return new PaginaPromocoes(numero, quantidadeCards, promocoes);
    }

    private List<PromocaoEncontrada> promocoes(int quantidade) {
        List<PromocaoEncontrada> promocoes = new ArrayList<>();
        for (int numero = 1; numero <= quantidade; numero++) {
            promocoes.add(promocao("MLB" + numero));
        }
        return List.copyOf(promocoes);
    }

    private PromocaoEncontrada promocao(String idExterno) {
        return new PromocaoEncontrada(
                "Notebook", null, "informatica",
                new BigDecimal("100.00"), new BigDecimal("80.00"),
                "https://produto.mercadolivre.com.br/" + idExterno, idExterno);
    }
}
