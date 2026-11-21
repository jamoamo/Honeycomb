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
package io.github.jamoamo.honeycomb.rest.auth;

import io.github.jamoamo.honeycomb.rest.FakeRestRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link BearerTokenExtractor} and {@link AuthenticatedPrincipal}.
 *
 * @author James Amoore
 */
@DisplayName("BearerTokenExtractor")
public class BearerTokenExtractorTest
{
   private final BearerTokenExtractor extractor = new BearerTokenExtractor();

   @Test
   @DisplayName("extracts the token, matching the scheme and header case-insensitively")
   public void testExtracts()
   {
      assertThat(extractor.extract(request(List.of("Bearer abc.def")))).contains("abc.def");
      assertThat(extractor.extract(request(List.of("  bEaReR   abc  ")))).contains("abc");
      assertThat(extractor.extract(new FakeRestRequest(
         "GET", "/", Map.of(), Map.of("authorization", List.of("Bearer x")), ""))).contains("x");
   }

   @Test
   @DisplayName("yields no credential without an Authorization header")
   public void testAbsent()
   {
      assertThat(extractor.extract(new FakeRestRequest("GET", "/"))).isEmpty();
   }

   @Test
   @DisplayName("rejects other schemes, empty tokens and repeated headers")
   public void testMalformed()
   {
      for (List<String> values : List.of(List.of("Basic abc"), List.of("Bearer"), List.of("Bearer   "),
         List.of("Bearer a", "Bearer b")))
      {
         assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> extractor.extract(request(values)));
      }
   }

   @Test
   @DisplayName("copies and exposes principal values")
   public void testPrincipal()
   {
      Map<String, Object> claims = new HashMap<>();
      claims.put("nullable", null);
      AuthenticatedPrincipal principal = new AuthenticatedPrincipal("s", Set.of("a"), Set.of("r"), claims);

      assertThat(principal.hasScope("a")).isTrue();
      assertThat(principal.hasScope("b")).isFalse();
      assertThat(principal.hasRole("r")).isTrue();
      assertThat(principal.hasRole("x")).isFalse();
      assertThat(principal.claims()).containsKey("nullable");
      assertThatExceptionOfType(UnsupportedOperationException.class)
         .isThrownBy(() -> principal.claims().put("k", "v"));
      assertThat(AuthenticatedPrincipal.of("s").scopes()).isEmpty();
   }

   @Test
   @DisplayName("rejects invalid principal values")
   public void testPrincipalValidation()
   {
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> AuthenticatedPrincipal.of(null));
      assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> AuthenticatedPrincipal.of(" "));
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new AuthenticatedPrincipal("s", null, Set.of(), Map.of()));
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new AuthenticatedPrincipal("s", Set.of(), null, Map.of()));
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new AuthenticatedPrincipal("s", Set.of(), Set.of(), null));
   }

   @Test
   @DisplayName("keeps the cause of an authentication failure")
   public void testExceptionCause()
   {
      IllegalStateException cause = new IllegalStateException("x");
      assertThat(new AuthenticationException("m", cause)).hasCause(cause).hasMessage("m");
   }

   private static FakeRestRequest request(final List<String> authorization)
   {
      return new FakeRestRequest("GET", "/", Map.of(), Map.of(BearerTokenExtractor.AUTHORIZATION, authorization), "");
   }
}
