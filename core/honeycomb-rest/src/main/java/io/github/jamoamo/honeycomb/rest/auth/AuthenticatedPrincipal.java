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

import io.github.jamoamo.honeycomb.rest.intercept.AttributeKey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The caller of an endpoint, as established by a {@link TokenAuthenticator}.
 *
 * @author James Amoore
 * @param subject the identifier of the caller, for example the {@code sub} claim of a JWT
 * @param scopes  the scopes granted to the caller
 * @param roles   the roles held by the caller
 * @param claims  every claim of the credential the caller presented, for application-specific checks
 * @since 1.1.0
 */
public record AuthenticatedPrincipal(
   String subject,
   Set<String> scopes,
   Set<String> roles,
   Map<String, Object> claims)
{
   /**
    * The key under which the {@link AuthInterceptor} stores the principal in the per-request attributes.
    */
   public static final AttributeKey<AuthenticatedPrincipal> ATTRIBUTE =
      AttributeKey.of("authenticatedPrincipal", AuthenticatedPrincipal.class);

   /**
    * Constructor.
    *
    * @param subject the identifier of the caller; must not be blank
    * @param scopes  the scopes granted to the caller
    * @param roles   the roles held by the caller
    * @param claims  every claim of the presented credential; values may be {@code null}
    */
   public AuthenticatedPrincipal
   {
      Objects.requireNonNull(subject, "subject cannot be null");
      if (subject.isBlank())
      {
         throw new IllegalArgumentException("subject cannot be blank");
      }
      scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes cannot be null"));
      roles = Set.copyOf(Objects.requireNonNull(roles, "roles cannot be null"));
      claims = Collections.unmodifiableMap(
         new LinkedHashMap<>(Objects.requireNonNull(claims, "claims cannot be null")));
   }

   /**
    * Creates a principal with no scopes, roles or claims.
    *
    * @param subject the identifier of the caller
    * @return the principal
    */
   public static AuthenticatedPrincipal of(final String subject)
   {
      return new AuthenticatedPrincipal(subject, Set.of(), Set.of(), Map.of());
   }

   /**
    * Whether the caller has been granted the scope.
    *
    * @param scope the scope
    * @return {@code true} when the scope is granted
    */
   public boolean hasScope(final String scope)
   {
      return scopes.contains(scope);
   }

   /**
    * Whether the caller holds the role.
    *
    * @param role the role
    * @return {@code true} when the role is held
    */
   public boolean hasRole(final String role)
   {
      return roles.contains(role);
   }
}
