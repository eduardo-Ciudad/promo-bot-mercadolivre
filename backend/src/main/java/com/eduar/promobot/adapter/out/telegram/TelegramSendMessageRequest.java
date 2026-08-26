package com.eduar.promobot.adapter.out.telegram;

public record TelegramSendMessageRequest(
        String chat_id,
        String text,
        String parse_mode,
        // NUNCA true: cobranca acidental em Telegram Stars caso o campo seja habilitado.
        boolean allow_paid_broadcast
) {
}
