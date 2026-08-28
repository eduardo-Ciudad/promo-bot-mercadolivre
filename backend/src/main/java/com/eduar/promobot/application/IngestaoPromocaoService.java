package com.eduar.promobot.application;

import com.eduar.promobot.adapter.out.messaging.PromocaoPublisher;
import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.port.out.PromocaoRepository;
import org.springframework.stereotype.Service;

@Service
public class IngestaoPromocaoService {

    private final PromocaoRepository promocaoRepository;
    private final PromocaoPublisher promocaoPublisher;

    public IngestaoPromocaoService(PromocaoRepository promocaoRepository,
                                   PromocaoPublisher promocaoPublisher) {
        this.promocaoRepository = promocaoRepository;
        this.promocaoPublisher = promocaoPublisher;
    }

    public ResultadoIngestao ingerir(Promocao promocao) {
        if (promocaoRepository.existePorIdExterno(promocao.getIdExterno())) {
            return ResultadoIngestao.IGNORADA_DUPLICATA;
        }

        promocaoRepository.salvar(promocao);
        promocaoPublisher.publicarParaEnriquecimento(promocao);
        return ResultadoIngestao.ACEITA;
    }
}
