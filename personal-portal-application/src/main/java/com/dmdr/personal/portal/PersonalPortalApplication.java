package com.dmdr.personal.portal;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
//mvn spring-boot:run -pl personal-portal-application -Dspring-boot.run.profiles=dev
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
@EnableAdminServer
public class PersonalPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalPortalApplication.class, args);
	}

}
