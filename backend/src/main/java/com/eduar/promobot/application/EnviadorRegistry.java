package com.eduar.promobot.application;

import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.port.out.EnviadorDeMensagem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EnviadorRegistry {

    private final Map<CanalDistribuicao, EnviadorDeMensagem> enviadoresPorCanal;

    public EnviadorRegistry(List<EnviadorDeMensagem> enviadores) {
        this.enviadoresPorCanal = enviadores.stream()
                .collect(Collectors.toMap(EnviadorDeMensagem::canal, Function.identity()));
    }

    public Optional<EnviadorDeMensagem> resolver(CanalDistribuicao canal) {
        return Optional.ofNullable(enviadoresPorCanal.get(canal));
    }
}
