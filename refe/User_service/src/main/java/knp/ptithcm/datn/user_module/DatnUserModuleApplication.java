package knp.ptithcm.datn.user_module;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class DatnUserModuleApplication {

	private static final Logger log = LoggerFactory.getLogger(DatnUserModuleApplication.class);

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DatnUserModuleApplication.class);
		app.run(args);
	}

	@Bean
	public ApplicationRunner logStartup(Environment environment) {
		return args -> {
			String applicationName = environment.getProperty("spring.application.name", "datn_user_module");
			String httpPort = environment.getProperty("server.port", "unknown");
			String grpcPort = environment.getProperty("spring.grpc.server.port", "unknown");
			String databaseUrl = environment.getProperty("spring.datasource.url", "not-set");
			String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto", "none");
			String keycloakServerUrl = environment.getProperty("keycloak.auth-server-url", "not-set");
			String keycloakRealm = environment.getProperty("keycloak.realm", "not-set");
			String keycloakClientId = environment.getProperty("keycloak.resource", "not-set");
			String[] activeProfiles = environment.getActiveProfiles();

			log.info("Application '{}' started", applicationName);
			log.info("Active profiles: {}", Arrays.toString(activeProfiles));
			log.info("HTTP server port: {}", httpPort);
			log.info("gRPC server port: {}", grpcPort);
			log.info("JPA hibernate.ddl-auto: {}", ddlAuto);
			log.info("Datasource URL: {}", databaseUrl);
			log.info("Keycloak server: {} | realm: {} | clientId: {}", keycloakServerUrl, keycloakRealm, keycloakClientId);
		};
	}

	@Bean
	public ApplicationRunner verifyKeycloakPermissions(Keycloak keycloak, Environment environment) {
		return args -> {
			String realm = environment.getProperty("keycloak.realm", "");
			try {
				UsersResource users = keycloak.realm(realm).users();
				int count = users.count();
				log.debug("[Keycloak] Service account can access realm '{}' (users.count={})", realm, count);
			} catch (Exception ex) {
				log.error("[Keycloak] Permission check failed for realm '{}': {}. Likely missing 'realm-management' -> 'manage-users' role on service account, wrong realm, or invalid client credentials.", realm, ex.getMessage());
			}
		};
	}

}
