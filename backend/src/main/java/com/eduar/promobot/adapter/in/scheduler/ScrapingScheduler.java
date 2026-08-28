package com.eduar.promobot.adapter.in.scheduler;


import com.eduar.promobot.adapter.out.messaging.PromocaoPublisher;
import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.port.out.BuscadorDePromocoes;
import com.eduar.promobot.domain.port.out.PromocaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScrapingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScrapingScheduler.class);

    private final BuscadorDePromocoes buscadorDePromocoes;
    private final PromocaoPublisher promocaoPublisher;
    private final PromocaoRepository promocaoRepository;

    public ScrapingScheduler(BuscadorDePromocoes buscadorDePromocoes,
                             PromocaoPublisher promocaoPublisher,
                             PromocaoRepository promocaoRepository) {
        this.buscadorDePromocoes = buscadorDePromocoes;
        this.promocaoPublisher = promocaoPublisher;
        this.promocaoRepository = promocaoRepository;
    }

    @Scheduled(fixedDelayString = "${scraping.scheduler.intervalo-ms}")
    public void executarScrapingAutomatico() {
        log.info("Iniciando execução automática do scraping");

        BuscadorDePromocoes.CriteriosBusca criterios = new BuscadorDePromocoes.CriteriosBusca(10, List.of());
        List<Promocao> encontradas = buscadorDePromocoes.buscarPromocoes(criterios);

        int novas = 0;
        for (Promocao promocao : encontradas) {
            if (promocaoRepository.existePorIdExterno(promocao.getIdExterno())) {
                continue;
            }
            promocaoRepository.salvar(promocao);
            promocaoPublisher.publicarParaEnriquecimento(promocao);
            novas++;
        }

        log.info("Scraping automático finalizado: {} promoções encontradas, {} novas publicadas", encontradas.size(), novas);
    }
}
