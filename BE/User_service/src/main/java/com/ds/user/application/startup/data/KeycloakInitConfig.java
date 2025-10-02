package com.ds.user.application.startup.data;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Configuration class for Keycloak initialization data
 */
@Configuration
@ConfigurationProperties(prefix = "keycloak.init")
@Getter
public class KeycloakInitConfig {
    
    private boolean enabled = true;
    private MasterConfig master = new MasterConfig();
    private List<RealmConfig> realms = List.of();
    private DefaultConfig defaultConfig = new DefaultConfig();
    
    @Getter
    public static class MasterConfig {
        private String realm = "master";
        private String username = "dev";
        private String password = "dev";
        
        public void setRealm(String realm) {
            this.realm = realm;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    @Getter
    public static class RealmConfig {
        private String name;
        private String displayName;
        private boolean enabled = true;
        private List<ClientConfig> clients = List.of();
        private List<RoleConfig> roles = List.of();
        private List<UserConfig> users = List.of();
        
        public void setName(String name) {
            this.name = name;
        }
        
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public void setClients(List<ClientConfig> clients) {
            this.clients = clients;
        }
        
        public void setRoles(List<RoleConfig> roles) {
            this.roles = roles;
        }
        
        public void setUsers(List<UserConfig> users) {
            this.users = users;
        }
    }
    
    @Getter
    public static class ClientConfig {
        private String clientId;
        private String name;
        private String secret;
        private boolean publicClient = false;
        private boolean serviceAccountsEnabled = false;
        private boolean standardFlowEnabled = true;
        private boolean directAccessGrantsEnabled = true;
        private List<String> redirectUris = List.of("*");
        private List<String> webOrigins = List.of("*");
        private List<String> roles = List.of();
        
        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public void setSecret(String secret) {
            this.secret = secret;
        }
        
        public void setPublicClient(boolean publicClient) {
            this.publicClient = publicClient;
        }
        
        public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
            this.serviceAccountsEnabled = serviceAccountsEnabled;
        }
        
        public void setStandardFlowEnabled(boolean standardFlowEnabled) {
            this.standardFlowEnabled = standardFlowEnabled;
        }
        
        public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
            this.directAccessGrantsEnabled = directAccessGrantsEnabled;
        }
        
        public void setRedirectUris(List<String> redirectUris) {
            this.redirectUris = redirectUris;
        }
        
        public void setWebOrigins(List<String> webOrigins) {
            this.webOrigins = webOrigins;
        }
        
        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
    
    @Getter
    public static class RoleConfig {
        private String name;
        private String description;
        
        public void setName(String name) {
            this.name = name;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
    
    @Getter
    public static class UserConfig {
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String password;
        private boolean enabled = true;
        private boolean emailVerified = true;
        private List<String> realmRoles = List.of();
        private Map<String, List<String>> clientRoles = Map.of();
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public void setEmailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
        }
        
        public void setRealmRoles(List<String> realmRoles) {
            this.realmRoles = realmRoles;
        }
        
        public void setClientRoles(Map<String, List<String>> clientRoles) {
            this.clientRoles = clientRoles;
        }
    }
    
    @Getter
    public static class DefaultConfig {
        private String realm = "delivery-system";
        private String clientId = "frontend-client";
        
        public void setRealm(String realm) {
            this.realm = realm;
        }
        
        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setMaster(MasterConfig master) {
        this.master = master;
    }
    
    public void setRealms(List<RealmConfig> realms) {
        this.realms = realms;
    }
    
    public void setDefaultConfig(DefaultConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }
}
