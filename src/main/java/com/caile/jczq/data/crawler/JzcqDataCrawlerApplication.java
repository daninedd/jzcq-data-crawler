package com.caile.jczq.data.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories({ "com.caile.jczq.data.crawler" })
@EntityScan({ "com.caile.jczq.data.crawler", "com.caile.jczq.data.crawler" })
public class JzcqDataCrawlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(JzcqDataCrawlerApplication.class, args);
	}
}
