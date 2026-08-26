package com.eduar.promobot.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EnvioMensagensScheduler {

    private final EnviarMensagensPendentesUseCase useCase;

    public EnvioMensagensScheduler(EnviarMensagensPendentesUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedDelayString = "${outbox.intervalo-scheduler-ms:5000}")
    public void executar() {
        useCase.processarLote();
    }
}
