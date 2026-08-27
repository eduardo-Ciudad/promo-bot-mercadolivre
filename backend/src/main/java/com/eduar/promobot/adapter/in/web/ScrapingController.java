package com.eduar.promobot.adapter.in.web;

import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.port.out.BuscadorDePromocoes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ScrapingController {

    private static final Logger log = LoggerFactory.getLogger(ScrapingController.class);

    private final BuscadorDePromocoes buscadorDePromocoes;

    public ScrapingController(BuscadorDePromocoes buscadorDePromocoes) {
        this.buscadorDePromocoes = buscadorDePromocoes;
    }

    @PostMapping("/admin/scraping/executar")
    public String executar() {
        BuscadorDePromocoes.CriteriosBusca criterios = new BuscadorDePromocoes.CriteriosBusca(10, List.of());

        List<Promocao> encontradas = buscadorDePromocoes.buscarPromocoes(criterios);
        log.info("Scraping executado manualmente. Promoções encontradas: {}", encontradas.size());
        for (Promocao promocao : encontradas) {
            log.info("Promoção encontrada: {} - R$ {}", promocao.getProduto().getNome(), promocao.getPrecoPromocional());
        }
        return "Encontradas " + encontradas.size() + " promoções. Veja o console para detalhes.";
    }
}
