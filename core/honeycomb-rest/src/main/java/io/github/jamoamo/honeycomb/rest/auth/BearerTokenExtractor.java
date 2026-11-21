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

import io.github.jamoamo.honeycomb.rest.request.RestRequest;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Extracts an RFC 6750 bearer token from the {@code Authorization} header.
 *
 * <p>
 * The scheme is matched case-insensitively. An absent header yields no credential; a header using another scheme,
 * a bearer header with no token, or more than one {@code Authorization} header is rejected as malformed.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class BearerTokenExtractor implements CredentialExtractor
{
   /**
    * The {@code Authorization} header name.
    */
   public static final String AUTHORIZATION = "Authorization";

   private static final String BEARER_PREFIX = "bearer ";

   @Override
   public Optional<String> extract(final RestRequest request)
   {
      List<String> values = request.headerValues(AUTHORIZATION);
      if (values.isEmpty())
      {
         return Optional.empty();
      }
      if (values.size() > 1)
      {
         throw new AuthenticationException("More than one Authorization header was supplied");
      }

      String value = values.getFirst().strip();
      if (!value.toLowerCase(Locale.ROOT).startsWith(BEARER_PREFIX))
      {
         throw new AuthenticationException("The Authorization header does not use the Bearer scheme");
      }

      String token = value.substring(BEARER_PREFIX.length()).strip();
      if (token.isEmpty())
      {
         throw new AuthenticationException("The Authorization header carries no bearer token");
      }
      return Optional.of(token);
   }
}
