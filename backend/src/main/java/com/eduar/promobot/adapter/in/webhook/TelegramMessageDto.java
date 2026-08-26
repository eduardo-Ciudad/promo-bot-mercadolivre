package com.eduar.promobot.adapter.in.webhook;

public record TelegramMessageDto(
        TelegramChatDto chat,
        String text,
        TelegramUserDto from
) {
}
