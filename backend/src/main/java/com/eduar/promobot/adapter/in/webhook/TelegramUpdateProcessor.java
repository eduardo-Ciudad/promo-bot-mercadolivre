package com.eduar.promobot.adapter.in.webhook;

import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.model.DestinoDistribuicao;
import com.eduar.promobot.domain.model.TipoDestino;
import com.eduar.promobot.domain.port.out.DestinoDistribuicaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramUpdateProcessor {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateProcessor.class);

    private final TelegramUpdateProcessadoRepository updateRepository;
    private final DestinoDistribuicaoRepository destinoRepository;

    public TelegramUpdateProcessor(TelegramUpdateProcessadoRepository updateRepository,
                                    DestinoDistribuicaoRepository destinoRepository) {
        this.updateRepository = updateRepository;
        this.destinoRepository = destinoRepository;
    }

    @Async
    public void processar(TelegramUpdateDto update) {
        processarSincrono(update);
    }

    @Transactional
    void processarSincrono(TelegramUpdateDto update) {
        if (jaProcessado(update.update_id())) {
            log.info("Update {} do Telegram ja processado, ignorando", update.update_id());
            return;
        }

        TelegramMessageDto message = update.message();
        if (message == null || message.chat() == null) {
            return;
        }

        if ("/start".equals(message.text())) {
            registrarOuReativarDestino(String.valueOf(message.chat().id()), tipoDestino(message.chat().type()));
        }
    }

    private boolean jaProcessado(long updateId) {
        if (updateRepository.existsById(updateId)) {
            return true;
        }
        try {
            updateRepository.save(new TelegramUpdateProcessado(updateId));
            return false;
        } catch (DataIntegrityViolationException e) {
            return true;
        }
    }

    private void registrarOuReativarDestino(String externalId, TipoDestino tipo) {
        destinoRepository.buscarPorExternalIdECanal(externalId, CanalDistribuicao.TELEGRAM)
                .ifPresentOrElse(
                        destino -> {
                            destino.reativar();
                            destinoRepository.salvar(destino);
                        },
                        () -> destinoRepository.salvar(
                                DestinoDistribuicao.builder()
                                        .canal(CanalDistribuicao.TELEGRAM)
                                        .externalId(externalId)
                                        .tipo(tipo)
                                        .build())
                );
    }

    private TipoDestino tipoDestino(String chatType) {
        if (chatType == null) {
            return TipoDestino.USUARIO;
        }
        return switch (chatType) {
            case "group", "supergroup" -> TipoDestino.GRUPO;
            case "channel" -> TipoDestino.CANAL;
            default -> TipoDestino.USUARIO;
        };
    }
}
