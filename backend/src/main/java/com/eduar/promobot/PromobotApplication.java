package com.eduar.promobot;

import com.eduar.promobot.config.OutboxProperties;
import com.eduar.promobot.config.TelegramProperties;
import com.eduar.promobot.config.WhatsAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({WhatsAppProperties.class, TelegramProperties.class, OutboxProperties.class})
public class PromobotApplication {

	public static void main(String[] args) {
		SpringApplication.run(PromobotApplication.class, args);
	}

}
