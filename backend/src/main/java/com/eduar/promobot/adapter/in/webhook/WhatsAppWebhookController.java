package com.eduar.promobot.adapter.in.webhook;


import com.eduar.promobot.config.WhatsAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppProperties properties;

    public WhatsAppWebhookController(WhatsAppProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ResponseEntity<String> verificarWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {

        log.info("Webhook verification received — mode: {}", mode);

        if ("subscribe".equals(mode) && properties.webhookVerifyToken().equals(verifyToken)) {
            log.info("Webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }

        log.warn("Webhook verification failed — token mismatch");
        return ResponseEntity.status(403).body("Verification failed");
    }

    @PostMapping
    public ResponseEntity<Void> receberEvento(@RequestBody String payload) {

        log.info("Webhook event received: {}", payload);

        return ResponseEntity.ok().build();

    }
}
