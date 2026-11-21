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

import io.github.jamoamo.honeycomb.rest.auth.AuthenticatedPrincipal;
import io.github.jamoamo.honeycomb.rest.auth.AuthenticationException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JwtTokenAuthenticator} with static keys.
 *
 * @author James Amoore
 */
@DisplayName("JwtTokenAuthenticator")
public class JwtTokenAuthenticatorTest
{
   private static final RSAKey RSA = Tokens.rsa("k1");
   private static final RSAKey OTHER_RSA = Tokens.rsa("k2");

   private static JwtTokenAuthenticator rsaAuthenticator() throws JOSEException
   {
      return JwtTokenAuthenticator.builder().publicKey(Tokens.pem(RSA.toPublicKey())).build();
   }

   @Test
   @DisplayName("accepts a valid RSA-signed token and maps its claims")
   public void testValidRsaToken() throws Exception
   {
      String token = Tokens.sign(RSA, Tokens.claims()
         .claim("scope", "orders:read orders:write")
         .claim("roles", List.of("admin"))
         .claim("tenant", "t1")
         .build());

      AuthenticatedPrincipal principal = rsaAuthenticator().authenticate(token);

      assertThat(principal.subject()).isEqualTo("james");
      assertThat(principal.scopes()).containsExactlyInAnyOrder("orders:read", "orders:write");
      assertThat(principal.roles()).containsExactly("admin");
      assertThat(principal.claims()).containsEntry("tenant", "t1");
   }

   @Test
   @DisplayName("rejects expired tokens, allowing for clock skew")
   public void testExpiry() throws Exception
   {
      JwtTokenAuthenticator authenticator = rsaAuthenticator();
      String recentlyExpired = Tokens.sign(RSA, Tokens.claims().expirationTime(ago(30)).build());
      String longExpired = Tokens.sign(RSA, Tokens.claims().expirationTime(ago(120)).build());

      assertThat(authenticator.authenticate(recentlyExpired).subject()).isEqualTo("james");
      assertThatExceptionOfType(AuthenticationException.class)
         .isThrownBy(() -> authenticator.authenticate(longExpired));

      JwtTokenAuthenticator strict = JwtTokenAuthenticator.builder()
         .publicKey(RSA.toPublicKey()).clockSkew(Duration.ZERO).build();
      assertThatExceptionOfType(AuthenticationException.class)
         .isThrownBy(() -> strict.authenticate(recentlyExpired));
   }

   @Test
   @DisplayName("rejects tokens without exp and tokens not yet valid")
   public void testRequiredAndNotBefore() throws Exception
   {
      JwtTokenAuthenticator authenticator = rsaAuthenticator();
      String noExpiry = Tokens.sign(RSA, new JWTClaimsSet.Builder().subject("james").build());
      String notYet = Tokens.sign(RSA, Tokens.claims().notBeforeTime(Date.from(Instant.now().plusSeconds(600))).build());

      assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> authenticator.authenticate(noExpiry));
      assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> authenticator.authenticate(notYet));
   }

   @Test
   @DisplayName("checks the issuer and audience when configured")
   public void testIssuerAndAudience() throws Exception
   {
      JwtTokenAuthenticator authenticator = JwtTokenAuthenticator.builder()
         .publicKey(RSA.toPublicKey())
         .issuer("https://issuer")
         .audiences("api", "other")
         .build();

      String good = Tokens.sign(RSA, Tokens.claims().issuer("https://issuer").audience(List.of("x", "api")).build());
      String wrongIssuer = Tokens.sign(RSA, Tokens.claims().issuer("https://evil").audience("api").build());
      String noIssuer = Tokens.sign(RSA, Tokens.claims().audience("api").build());
      String wrongAudience = Tokens.sign(RSA, Tokens.claims().issuer("https://issuer").audience("x").build());
      String noAudience = Tokens.sign(RSA, Tokens.claims().issuer("https://issuer").build());

      assertThat(authenticator.authenticate(good).subject()).isEqualTo("james");
      for (String token : List.of(wrongIssuer, noIssuer, wrongAudience, noAudience))
      {
         assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> authenticator.authenticate(token));
      }
   }

   @Test
   @DisplayName("rejects bad signatures, unaccepted algorithms, unsigned and malformed tokens")
   public void testRejectsForgeries() throws Exception
   {
      JwtTokenAuthenticator authenticator = rsaAuthenticator();
      JWTClaimsSet claims = Tokens.claims().build();
      List<String> forgeries = List.of(
         Tokens.sign(OTHER_RSA, claims),
         Tokens.sign(RSA, JWSAlgorithm.RS384, claims),
         Tokens.signHmac(RSA.toPublicKey().getEncoded(), JWSAlgorithm.HS256, claims),
         new PlainJWT(new PlainHeader(), claims).serialize(),
         "not-a-jwt",
         "a.b.c");

      for (String token : forgeries)
      {
         assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> authenticator.authenticate(token));
      }
   }

   @Test
   @DisplayName("rejects tokens that do not identify a caller")
   public void testRequiresSubject() throws Exception
   {
      JwtTokenAuthenticator authenticator = rsaAuthenticator();
      String noSubject = Tokens.sign(RSA, Tokens.claims().subject(null).build());
      String blankSubject = Tokens.sign(RSA, Tokens.claims().subject(" ").build());

      assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> authenticator.authenticate(noSubject));
      assertThatExceptionOfType(AuthenticationException.class)
         .isThrownBy(() -> authenticator.authenticate(blankSubject));
   }

   @Test
   @DisplayName("accepts the configured RSA algorithms only")
   public void testConfiguredRsaAlgorithms() throws Exception
   {
      JwtTokenAuthenticator authenticator = JwtTokenAuthenticator.builder()
         .publicKey(RSA.toPublicKey()).algorithms("RS384", "PS256").build();

      assertThat(authenticator.authenticate(Tokens.sign(RSA, JWSAlgorithm.PS256, Tokens.claims().build())).subject())
         .isEqualTo("james");
      assertThatExceptionOfType(AuthenticationException.class)
         .isThrownBy(() -> authenticator.authenticate(Tokens.sign(RSA, Tokens.claims().build())));
   }

   @Test
   @DisplayName("verifies EC keys with the algorithm matching their curve")
   public void testEcKeys() throws Exception
   {
      ECKey p256 = Tokens.ec(Curve.P_256);
      ECKey p384 = Tokens.ec(Curve.P_384);

      JwtTokenAuthenticator authenticator =
         JwtTokenAuthenticator.builder().publicKey(Tokens.pem(p256.toPublicKey())).build();
      assertThat(authenticator.authenticate(Tokens.sign(p256, JWSAlgorithm.ES256, Tokens.claims().build())).subject())
         .isEqualTo("james");

      JwtTokenAuthenticator es384 = JwtTokenAuthenticator.builder().publicKey(p384.toECPublicKey()).build();
      assertThat(es384.authenticate(Tokens.sign(p384, JWSAlgorithm.ES384, Tokens.claims().build())).subject())
         .isEqualTo("james");

      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
         JwtTokenAuthenticator.builder().publicKey(p256.toPublicKey()).algorithms("ES384").build());
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
         JwtTokenAuthenticator.builder().publicKey(p256.toPublicKey()).algorithms("RS256").build());
   }

   @Test
   @DisplayName("verifies HMAC tokens with a long enough secret")
   public void testSecret()
   {
      JwtTokenAuthenticator authenticator = JwtTokenAuthenticator.builder().secret(Tokens.SECRET).build();

      assertThat(authenticator.authenticate(Tokens.signHmac(Tokens.SECRET, JWSAlgorithm.HS256, Tokens.claims().build()))
         .subject()).isEqualTo("james");
      assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> authenticator.authenticate(
         Tokens.sign(RSA, Tokens.claims().build())));

      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> JwtTokenAuthenticator.builder().secret(new byte[16]).build());
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> JwtTokenAuthenticator.builder().secret(Tokens.SECRET).algorithms("HS512").build());
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> JwtTokenAuthenticator.builder().secret(Tokens.SECRET).algorithms("RS256").build());
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> JwtTokenAuthenticator.builder().secret(Tokens.SECRET).algorithms("none").build());
   }

   @Test
   @DisplayName("requires exactly one usable key source")
   public void testKeySourceValidation() throws Exception
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> JwtTokenAuthenticator.builder().build());
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> JwtTokenAuthenticator.builder().publicKey(RSA.toPublicKey()).secret(Tokens.SECRET).build());
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> JwtTokenAuthenticator.builder()
         .publicKey(new RSAKeyGenerator(1024, true).generate().toPublicKey())
         .build());

      KeyPairGenerator ed = KeyPairGenerator.getInstance("Ed25519");
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
         JwtTokenAuthenticator.builder().publicKey(ed.generateKeyPair().getPublic()).build());
   }

   @Test
   @DisplayName("rejects invalid durations and nulls")
   public void testBuilderValidation()
   {
      JwtTokenAuthenticatorBuilder builder = JwtTokenAuthenticator.builder();

      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> builder.clockSkew(Duration.ofSeconds(-1)));
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> builder.jwksCacheTtl(Duration.ofSeconds(-1)));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> builder.jwksMinRefreshInterval(null));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> builder.claimsMapper(null));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> builder.issuer(null));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> builder.jwksUri(null));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> builder.publicKey((String) null));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> builder.secret(null));
   }

   @Test
   @DisplayName("rejects a claims mapper that returns null")
   public void testNullMapperResult() throws Exception
   {
      JwtTokenAuthenticator authenticator = JwtTokenAuthenticator.builder()
         .publicKey(RSA.toPublicKey()).claimsMapper(claims -> null).build();

      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> authenticator.authenticate(Tokens.sign(RSA, Tokens.claims().build())));
   }

   private static Date ago(final long seconds)
   {
      return Date.from(Instant.now().minusSeconds(seconds));
   }
}
