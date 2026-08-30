package com.eduar.promobot.adapter.out.telegram;

import com.eduar.promobot.config.TelegramProperties;
import com.eduar.promobot.domain.exception.DestinatarioIndisponivelException;
import com.eduar.promobot.domain.exception.EnvioInvalidoException;
import com.eduar.promobot.domain.exception.RateLimitedException;
import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.model.MensagemSaida;
import com.eduar.promobot.domain.model.ResultadoEnvio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramBotAdapterTest {

    private static final TelegramProperties PROPERTIES =
            new TelegramProperties("fake-token", "https://api.telegram.org", "fake-secret");

    private MockRestServiceServer server;
    private TelegramBotAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.adapter = new TelegramBotAdapter(builder, PROPERTIES, JsonMapper.builder().build());
    }

    private MensagemSaida mensagemSemImagem() {
        return new MensagemSaida(UUID.randomUUID(), CanalDistribuicao.TELEGRAM, "123456", "Oferta imperdivel",
                null, URI.create("https://exemplo.com/produto"), "Produto Top",
                new BigDecimal("199.90"), new BigDecimal("149.90"));
    }

    private MensagemSaida mensagemComImagem() {
        return new MensagemSaida(UUID.randomUUID(), CanalDistribuicao.TELEGRAM, "123456", "Oferta imperdivel",
                URI.create("https://exemplo.com/imagem.jpg"), URI.create("https://exemplo.com/produto"), "Produto Top",
                new BigDecimal("199.90"), new BigDecimal("149.90"));
    }

    @Test
    void deveEnviarSendMessageQuandoNaoHaImagem() {
        server.expect(requestTo("https://api.telegram.org/botfake-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"allow_paid_broadcast\":true"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"allow_paid_broadcast\":false")))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":42}}", MediaType.APPLICATION_JSON));

        ResultadoEnvio resultado = adapter.enviar(mensagemSemImagem());

        assertThat(resultado.providerMessageId()).isEqualTo("42");
        server.verify();
    }

    @Test
    void deveEnviarSendPhotoQuandoHaImagem() {
        server.expect(requestTo("https://api.telegram.org/botfake-token/sendPhoto"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"allow_paid_broadcast\":false")))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":99}}", MediaType.APPLICATION_JSON));

        ResultadoEnvio resultado = adapter.enviar(mensagemComImagem());

        assertThat(resultado.providerMessageId()).isEqualTo("99");
        server.verify();
    }

    @Test
    void deveLancarRateLimitedExceptionQuando429() {
        server.expect(requestTo("https://api.telegram.org/botfake-token/sendMessage"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"error_code\":429,\"description\":\"Too Many Requests\",\"parameters\":{\"retry_after\":30}}"));

        assertThatThrownBy(() -> adapter.enviar(mensagemSemImagem()))
                .isInstanceOf(RateLimitedException.class)
                .satisfies(e -> assertThat(((RateLimitedException) e).getRetryAfterSeconds()).isEqualTo(30L));
    }

    @Test
    void deveLancarDestinatarioIndisponivelExceptionQuando403() {
        server.expect(requestTo("https://api.telegram.org/botfake-token/sendMessage"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}"));

        assertThatThrownBy(() -> adapter.enviar(mensagemSemImagem()))
                .isInstanceOf(DestinatarioIndisponivelException.class);
    }

    @Test
    void deveLancarEnvioInvalidoExceptionQuando400() {
        server.expect(requestTo("https://api.telegram.org/botfake-token/sendMessage"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: chat not found\"}"));

        assertThatThrownBy(() -> adapter.enviar(mensagemSemImagem()))
                .isInstanceOf(EnvioInvalidoException.class);
    }
}
