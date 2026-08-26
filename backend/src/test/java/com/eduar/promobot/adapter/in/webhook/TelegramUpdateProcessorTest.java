package com.eduar.promobot.adapter.in.webhook;

import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.port.out.DestinoDistribuicaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramUpdateProcessorTest {

    @Mock
    private TelegramUpdateProcessadoRepository updateRepository;

    @Mock
    private DestinoDistribuicaoRepository destinoRepository;

    private TelegramUpdateProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TelegramUpdateProcessor(updateRepository, destinoRepository);
    }

    private TelegramUpdateDto update(long updateId) {
        return new TelegramUpdateDto(updateId,
                new TelegramMessageDto(new TelegramChatDto(555L, "private"), "/start", null));
    }

    @Test
    void deveIgnorarUpdateJaProcessado() {
        when(updateRepository.existsById(100L)).thenReturn(true);

        processor.processarSincrono(update(100L));

        verify(updateRepository, never()).save(any());
        verifyNoInteractions(destinoRepository);
    }

    @Test
    void deveIgnorarUpdateProcessadoConcorrentemente() {
        when(updateRepository.existsById(100L)).thenReturn(false);
        when(updateRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        processor.processarSincrono(update(100L));

        verifyNoInteractions(destinoRepository);
    }

    @Test
    void deveRegistrarDestinoNoPrimeiroStart() {
        when(updateRepository.existsById(100L)).thenReturn(false);
        when(updateRepository.save(any())).thenReturn(new TelegramUpdateProcessado(100L));
        when(destinoRepository.buscarPorExternalIdECanal(eq("555"), eq(CanalDistribuicao.TELEGRAM)))
                .thenReturn(Optional.empty());

        processor.processarSincrono(update(100L));

        verify(destinoRepository).salvar(any());
    }
}
