package com.tradebot.upstox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UpstoxOptionChainApplication {

	public static void main(String[] args) {
		SpringApplication.run(UpstoxOptionChainApplication.class, args);
	}
}
