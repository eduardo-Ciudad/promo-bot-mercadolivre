package com.eduar.promobot;

import com.eduar.promobot.config.WhatsAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WhatsAppProperties.class)
public class PromobotApplication {

	public static void main(String[] args) {
		SpringApplication.run(PromobotApplication.class, args);
	}

}
