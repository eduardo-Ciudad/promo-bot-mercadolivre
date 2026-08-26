package com.eduar.promobot.adapter.in.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TelegramWebhookController.class)
@TestPropertySource(properties = {
        "telegram.bot-token=fake-token",
        "telegram.webhook-secret-token=segredo-correto"
})
class TelegramWebhookControllerTest {

    private static final String HEADER = "X-Telegram-Bot-Api-Secret-Token";
    private static final String SECRET = "segredo-correto";
    private static final String PAYLOAD = """
            {"update_id": 100, "message": {"chat": {"id": 999, "type": "private"}, "text": "/start"}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelegramUpdateProcessor processor;

    @Test
    void deveRejeitarQuandoHeaderAusente() throws Exception {
        mockMvc.perform(post("/webhook/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isForbidden());

        verify(processor, never()).processar(any());
    }

    @Test
    void deveRejeitarQuandoHeaderIncorreto() throws Exception {
        mockMvc.perform(post("/webhook/telegram")
                        .header(HEADER, "token-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isForbidden());

        verify(processor, never()).processar(any());
    }

    @Test
    void deveAceitarQuandoHeaderCorreto() throws Exception {
        mockMvc.perform(post("/webhook/telegram")
                        .header(HEADER, SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isOk());

        verify(processor).processar(any());
    }
}
