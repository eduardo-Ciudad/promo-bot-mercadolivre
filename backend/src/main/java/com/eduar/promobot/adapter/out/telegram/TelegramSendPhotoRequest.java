package com.eduar.promobot.adapter.out.telegram;

public record TelegramSendPhotoRequest(
        String chat_id,
        String photo,
        String caption,
        String parse_mode,
        // NUNCA true: cobranca acidental em Telegram Stars caso o campo seja habilitado.
        boolean allow_paid_broadcast
) {
}
