package com.eduar.promobot.adapter.out.telegram;

public record TelegramApiResponse(
        boolean ok,
        TelegramMessageResult result,
        Integer error_code,
        String description,
        TelegramResponseParameters parameters
) {
}
