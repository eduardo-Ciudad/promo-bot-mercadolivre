package com.eduar.promobot.scraperlocal.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import java.util.List;
import java.util.Objects;

public final class PlaywrightConfig {
    private PlaywrightConfig() {
    }

    public static Browser criarBrowser(Playwright playwright, ScraperConfig config) {
        Objects.requireNonNull(playwright, "playwright e obrigatorio");
        Objects.requireNonNull(config, "config e obrigatoria");
        return playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(config.headless())
                .setArgs(List.of("--disable-gpu", "--no-sandbox")));
    }
}

