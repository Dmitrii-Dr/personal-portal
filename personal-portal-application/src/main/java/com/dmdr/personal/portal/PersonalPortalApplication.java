package com.dmdr.personal.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PersonalPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalPortalApplication.class, args);
	}

}
