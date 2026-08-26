package com.eduar.promobot.adapter.out.persistence;

import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.model.MensagemOutbox;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MensagemOutboxRepositoryAdapterLeaseTest {

    @Test
    void deveReivindicarApenasMensagemComLeaseExpirado() throws Exception {
        MensagemOutbox expirada = mensagemReivindicadaEm(Instant.now().minusSeconds(121));
        MensagemOutbox vigente = mensagemReivindicadaEm(Instant.now().minusSeconds(60));
        MensagemOutboxJpaRepository jpaRepository = mock(MensagemOutboxJpaRepository.class);

        when(jpaRepository.reivindicarPendentes(10, 120)).thenAnswer(invocation -> {
            Instant limite = Instant.now().minusSeconds(invocation.getArgument(1, Long.class));
            return List.of(expirada, vigente).stream()
                    .filter(item -> item.getReivindicadoEm().isBefore(limite))
                    .toList();
        });

        MensagemOutboxRepositoryAdapter adapter = new MensagemOutboxRepositoryAdapter(jpaRepository);
        List<MensagemOutbox> elegiveis = adapter.buscarPendentesParaEnvio(10, 120);

        assertThat(elegiveis).containsExactly(expirada).doesNotContain(vigente);
    }

    private MensagemOutbox mensagemReivindicadaEm(Instant reivindicadoEm) throws Exception {
        MensagemOutbox mensagem = MensagemOutbox.builder()
                .promocaoId(UUID.randomUUID())
                .destinoId(UUID.randomUUID())
                .canal(CanalDistribuicao.TELEGRAM)
                .build();
        Field field = MensagemOutbox.class.getDeclaredField("reivindicadoEm");
        field.setAccessible(true);
        field.set(mensagem, reivindicadoEm);
        return mensagem;
    }
}
