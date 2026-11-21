/*
 * The MIT License
 *
 * Copyright 2026 James Amoore.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.jamoamo.honeycomb.auth.jwt;

import io.github.jamoamo.honeycomb.rest.auth.AuthenticationException;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JwtTokenAuthenticator} verifying against a JWKS endpoint.
 *
 * @author James Amoore
 */
@DisplayName("JwtTokenAuthenticator with a JWKS URI")
public class JwksTokenAuthenticatorTest
{
   private static final RSAKey KEY_1 = Tokens.rsa("k1");
   private static final RSAKey KEY_2 = Tokens.rsa("k2");
   private static final ECKey EC_KEY = Tokens.ec(Curve.P_256);

   private final AtomicReference<List<JWK>> published = new AtomicReference<>(List.of(KEY_1.toPublicJWK()));
   private final AtomicInteger fetches = new AtomicInteger();
   private HttpServer server;
   private URI jwksUri;

   @BeforeEach
   public void startServer() throws IOException
   {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/jwks", exchange -> {
         fetches.incrementAndGet();
         byte[] body = new JWKSet(published.get()).toString().getBytes(StandardCharsets.UTF_8);
         exchange.getResponseHeaders().add("Content-Type", "application/json");
         exchange.sendResponseHeaders(200, body.length);
         try (OutputStream out = exchange.getResponseBody())
         {
            out.write(body);
         }
      });
      server.start();
      jwksUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/jwks");
   }

   @AfterEach
   public void stopServer()
   {
      server.stop(0);
   }

   @Test
   @DisplayName("verifies tokens with published keys and caches the key set")
   public void testVerifiesAndCaches()
   {
      JwtTokenAuthenticator authenticator = JwtTokenAuthenticator.builder().jwksUri(jwksUri).build();

      assertThat(authenticator.authenticate(Tokens.sign(KEY_1, Tokens.claims().build())).subject()).isEqualTo("james");
      assertThat(authenticator.authenticate(Tokens.sign(KEY_1, Tokens.claims().build())).subject()).isEqualTo("james");
      assertThat(fetches).hasValue(1);
   }

   @Test
   @DisplayName("refreshes the key set when a token names an unknown key")
   public void testKeyRollover()
   {
      JwtTokenAuthenticator authenticator = JwtTokenAuthenticator.builder()
         .jwksUri(jwksUri).jwksMinRefreshInterval(Duration.ZERO).build();
      authenticator.authenticate(Tokens.sign(KEY_1, Tokens.claims().build()));

      published.set(List.of(KEY_1.toPublicJWK(), KEY_2.toPublicJWK()));

      assertThat(authenticator.authenticate(Tokens.sign(KEY_2, Tokens.claims().build())).subject()).isEqualTo("james");
   }

   @Test
   @DisplayName("rejects tokens signed by unpublished keys")
   public void testUnknownKey()
   {
      JwtTokenAuthenticator authenticator = JwtTokenAuthenticator.builder().jwksUri(jwksUri).build();

      assertThatExceptionOfType(AuthenticationException.class)
         .isThrownBy(() -> authenticator.authenticate(Tokens.sign(KEY_2, Tokens.claims().build())));
   }

   @Test
   @DisplayName("accepts configured EC algorithms from the key set")
   public void testEcFromJwks()
   {
      published.set(List.of(EC_KEY.toPublicJWK()));
      JwtTokenAuthenticator authenticator = JwtTokenAuthenticator.builder()
         .jwksUri(jwksUri).algorithms("ES256").build();

      assertThat(authenticator.authenticate(Tokens.sign(EC_KEY, JWSAlgorithm.ES256, Tokens.claims().build()))
         .subject()).isEqualTo("james");
   }

   @Test
   @DisplayName("treats an unreachable key set as a server failure, not the caller's")
   public void testUnreachable()
   {
      JwtTokenAuthenticator authenticator = JwtTokenAuthenticator.builder()
         .jwksUri(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/missing"))
         .build();
      server.removeContext("/jwks");

      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> authenticator.authenticate(Tokens.sign(KEY_1, Tokens.claims().build())));
   }

   @Test
   @DisplayName("rejects HMAC algorithms and invalid settings for a JWKS URI")
   public void testInvalidConfiguration()
   {
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
         JwtTokenAuthenticator.builder().jwksUri(jwksUri).algorithms("HS256").build());
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
         JwtTokenAuthenticator.builder().jwksUri(URI.create("urn:not-a-url")).build());
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> JwtTokenAuthenticator.builder()
         .jwksUri(jwksUri).jwksCacheTtl(Duration.ofSeconds(10)).jwksMinRefreshInterval(Duration.ofSeconds(20)).build());
   }
}
