package com.eduar.promobot.adapter.in.webhook;

public record TelegramUpdateDto(
        long update_id,
        TelegramMessageDto message
) {
}
