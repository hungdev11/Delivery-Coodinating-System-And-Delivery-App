package com.ds.gateway.application.config;

import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat configuration to prevent double chunked encoding issues with Cloudflare
 * Note: The main fix is in ResponseHeaderFilter which buffers responses to calculate Content-Length
 */
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                if (connector.getProtocolHandler() instanceof Http11NioProtocol protocol) {
                    // Enable keep-alive to improve connection handling
                    protocol.setUseKeepAliveResponseHeader(true);
                    // Compression is already disabled in application.yml
                    // Response buffering is handled by ResponseHeaderFilter
                }
            });
        };
    }
}
