package com.MPesaStkPush.MPesaStkPush;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.MPesaStkPush.MPesaStkPush.repository")
public class MPesaStkPushApplication {

	public static void main(String[] args) {
		SpringApplication.run(MPesaStkPushApplication.class, args);
	}

}
