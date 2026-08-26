package com.eduar.promobot.adapter.out.telegram;

public record TelegramResponseParameters(
        Long retry_after,
        Long migrate_to_chat_id
) {
}
