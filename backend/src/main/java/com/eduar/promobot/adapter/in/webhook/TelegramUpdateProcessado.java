package com.eduar.promobot.adapter.in.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "telegram_update_processado")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramUpdateProcessado {

    @Id
    @Column(name = "update_id")
    private Long updateId;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;

    public TelegramUpdateProcessado(Long updateId) {
        this.updateId = updateId;
        this.processadoEm = Instant.now();
    }

    public Long getUpdateId() {
        return updateId;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }
}
