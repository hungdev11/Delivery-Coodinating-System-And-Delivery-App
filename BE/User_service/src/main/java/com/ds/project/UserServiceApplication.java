package com.ds.project;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class UserServiceApplication {

	private static final Logger log = LoggerFactory.getLogger(UserServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(UserServiceApplication.class);
		app.run(args);
	}

	@Bean
	public ApplicationRunner logStartup(Environment environment) {
		return args -> {
			String applicationName = environment.getProperty("spring.application.name", "user_service");
			String httpPort = environment.getProperty("server.port", "unknown");
			String databaseUrl = environment.getProperty("spring.datasource.url", "not-set");
			String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto", "none");
			String keycloakServerUrl = environment.getProperty("keycloak.auth-server-url", "not-set");
			String keycloakRealm = environment.getProperty("keycloak.realm", "not-set");
			String keycloakClientId = environment.getProperty("keycloak.resource", "not-set");
			String[] activeProfiles = environment.getActiveProfiles();

			log.info("Application '{}' started", applicationName);
			log.info("Active profiles: {}", Arrays.toString(activeProfiles));
			log.info("HTTP REST server port: {}", httpPort);
			log.info("JPA hibernate.ddl-auto: {}", ddlAuto);
			log.info("Datasource URL: {}", databaseUrl);
			log.info("Keycloak server: {} | realm: {} | clientId: {}", keycloakServerUrl, keycloakRealm, keycloakClientId);
		};
	}
}
