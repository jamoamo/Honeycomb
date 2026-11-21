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

import com.nimbusds.jwt.JWTClaimsSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link DefaultClaimsMapper} and {@link PemKeys}.
 *
 * @author James Amoore
 */
@DisplayName("DefaultClaimsMapper")
public class DefaultClaimsMapperTest
{
   private final DefaultClaimsMapper mapper = new DefaultClaimsMapper();

   @Test
   @DisplayName("reads scp arrays and space-separated roles")
   public void testArraysAndStrings()
   {
      AuthenticatedPrincipal principal = mapper.map(new JWTClaimsSet.Builder()
         .subject("james").claim("scp", List.of("a", "b")).claim("roles", " admin  auditor ").build());

      assertThat(principal.scopes()).containsExactlyInAnyOrder("a", "b");
      assertThat(principal.roles()).containsExactlyInAnyOrder("admin", "auditor");
   }

   @Test
   @DisplayName("prefers the first scope claim present and defaults to no scopes or roles")
   public void testScopePrecedence()
   {
      assertThat(mapper.map(new JWTClaimsSet.Builder()
         .subject("james").claim("scope", "x").claim("scp", List.of("y")).build()).scopes()).containsExactly("x");

      AuthenticatedPrincipal bare = mapper.map(new JWTClaimsSet.Builder().subject("james").build());
      assertThat(bare.scopes()).isEmpty();
      assertThat(bare.roles()).isEmpty();
   }

   @Test
   @DisplayName("uses custom claim names")
   public void testCustomClaims()
   {
      DefaultClaimsMapper custom = new DefaultClaimsMapper("uid", List.of("permissions"), "groups");

      AuthenticatedPrincipal principal = custom.map(new JWTClaimsSet.Builder()
         .subject("ignored").claim("uid", "u1").claim("permissions", "p").claim("groups", List.of("g")).build());

      assertThat(principal.subject()).isEqualTo("u1");
      assertThat(principal.scopes()).containsExactly("p");
      assertThat(principal.roles()).containsExactly("g");
   }

   @Test
   @DisplayName("rejects claims of the wrong type")
   public void testWrongTypes()
   {
      for (JWTClaimsSet claims : List.of(
         new JWTClaimsSet.Builder().claim("sub", 42).build(),
         new JWTClaimsSet.Builder().subject("james").claim("scope", 42).build(),
         new JWTClaimsSet.Builder().subject("james").claim("roles", List.of(1, 2)).build()))
      {
         assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> mapper.map(claims));
      }
   }

   @Test
   @DisplayName("rejects null claim names")
   public void testNulls()
   {
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new DefaultClaimsMapper(null, List.of(), "r"));
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new DefaultClaimsMapper("s", null, "r"));
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new DefaultClaimsMapper("s", List.of(), null));
   }

   @Test
   @DisplayName("rejects PEM text without a supported public key")
   public void testInvalidPem()
   {
      for (String pem : List.of(
         "no pem here",
         "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----",
         "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----",
         "-----BEGIN CERTIFICATE-----\nAAAA\n-----END CERTIFICATE-----"))
      {
         assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> PemKeys.parsePublicKey(pem));
      }
   }
}
