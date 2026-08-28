package com.eduar.promobot.adapter.in.web;

import com.eduar.promobot.application.IngestaoPromocaoService;
import com.eduar.promobot.application.ResultadoIngestao;
import com.eduar.promobot.config.IngestaoProperties;
import com.eduar.promobot.domain.model.Promocao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class IngestaoPromocaoControllerTest {

    private static final String ENDPOINT = "/api/promocoes/ingestao";
    private static final String HEADER = "X-Ingestao-Api-Key";
    private static final String API_KEY = "segredo-correto";
    private static final String PAYLOAD_VALIDO = """
            {
              "produtoNome": "Notebook Gamer",
              "produtoImagemUrl": "https://exemplo.com/notebook.jpg",
              "produtoCategoria": "informatica",
              "precoOriginal": 5000.00,
              "precoPromocional": 4000.00,
              "linkOriginal": "https://exemplo.com/notebook",
              "idExterno": "MLB123456"
            }
            """;

    private IngestaoPromocaoService ingestaoPromocaoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ingestaoPromocaoService = mock(IngestaoPromocaoService.class);
        IngestaoProperties properties = new IngestaoProperties(API_KEY);
        IngestaoPromocaoController controller =
                new IngestaoPromocaoController(properties, ingestaoPromocaoService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void deveRetornarCreatedQuandoPromocaoForAceita() throws Exception {
        when(ingestaoPromocaoService.ingerir(any(Promocao.class)))
                .thenReturn(ResultadoIngestao.ACEITA);

        mockMvc.perform(post(ENDPOINT)
                        .header(HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("ACEITA"));

        verify(ingestaoPromocaoService).ingerir(any(Promocao.class));
    }

    @Test
    void deveRetornarOkQuandoPromocaoForDuplicada() throws Exception {
        when(ingestaoPromocaoService.ingerir(any(Promocao.class)))
                .thenReturn(ResultadoIngestao.IGNORADA_DUPLICATA);

        mockMvc.perform(post(ENDPOINT)
                        .header(HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("IGNORADA_DUPLICATA"));

        verify(ingestaoPromocaoService).ingerir(any(Promocao.class));
    }

    @Test
    void deveRejeitarComCorpoVazioQuandoHeaderEstiverAusente() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));

        verify(ingestaoPromocaoService, never()).ingerir(any(Promocao.class));
    }

    @Test
    void deveRejeitarComCorpoVazioQuandoApiKeyEstiverIncorreta() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(HEADER, "segredo-incorreto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));

        verify(ingestaoPromocaoService, never()).ingerir(any(Promocao.class));
    }

    @Test
    void deveRetornarBadRequestQuandoPayloadForInvalido() throws Exception {
        String payloadInvalido = """
                {
                  "produtoNome": "",
                  "precoOriginal": 0,
                  "precoPromocional": 4000.00,
                  "linkOriginal": "https://exemplo.com/notebook",
                  "idExterno": "MLB123456"
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .header(HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verify(ingestaoPromocaoService, never()).ingerir(any(Promocao.class));
    }
}
