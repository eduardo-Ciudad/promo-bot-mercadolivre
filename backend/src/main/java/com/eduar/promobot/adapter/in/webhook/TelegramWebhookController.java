package com.eduar.promobot.adapter.in.webhook;

import com.eduar.promobot.config.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/webhook/telegram")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);
    private static final String HEADER_SECRET_TOKEN = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramProperties properties;
    private final TelegramUpdateProcessor processor;

    public TelegramWebhookController(TelegramProperties properties, TelegramUpdateProcessor processor) {
        this.properties = properties;
        this.processor = processor;
    }

    @PostMapping
    public ResponseEntity<Void> receberUpdate(
            @RequestHeader(value = HEADER_SECRET_TOKEN, required = false) String secretToken,
            @RequestBody TelegramUpdateDto update) {

        if (!tokenValido(secretToken)) {
            log.warn("Webhook do Telegram recebido com secret token ausente ou invalido");
            return ResponseEntity.status(403).build();
        }

        processor.processar(update);
        return ResponseEntity.ok().build();
    }

    private boolean tokenValido(String recebido) {
        String esperado = properties.webhookSecretToken();
        if (esperado == null || esperado.isBlank() || recebido == null) {
            return false;
        }
        byte[] recebidoBytes = recebido.getBytes(StandardCharsets.UTF_8);
        byte[] esperadoBytes = esperado.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(recebidoBytes, esperadoBytes);
    }
}
