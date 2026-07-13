package com.jobready.gateway.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

/**
 * Pins the proxy HTTP client to HTTP/1.1.
 *
 * <p>The gateway forwards requests with the JDK {@link HttpClient} (the only client on the
 * classpath). Its default version is HTTP/2, so for cleartext {@code http://} upstreams it prefixes
 * every request with an h2c upgrade offer — {@code Connection: Upgrade, HTTP2-Settings},
 * {@code Upgrade: h2c} — and sends the body chunked.
 *
 * <p>Tomcat upstreams (auth, application, document) ignore the offer and read the body anyway.
 * uvicorn (genai, email) rejects the request instead: it logs "Unsupported upgrade request",
 * discards the body, and FastAPI then fails validation with "body: Field required". That made every
 * POST/PUT to a Python service fail while the Java ones worked.
 *
 * <p>None of our upstreams speak h2c, so the upgrade offer buys nothing — dropping it is what makes
 * the Python services reachable.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder() {
        return ClientHttpRequestFactoryBuilder.jdk()
                .withHttpClientCustomizer(builder -> builder.version(HttpClient.Version.HTTP_1_1));
    }
}
