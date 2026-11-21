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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The default {@link ClaimsMapper}.
 *
 * <p>
 * The subject is read from a string claim ({@code sub} by default). Scopes are read from the first scope claim
 * present ({@code scope}, then {@code scp}, by default) and roles from the roles claim ({@code roles} by default);
 * each may be a space-separated string or an array of strings. Every claim is also exposed on the principal.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class DefaultClaimsMapper implements ClaimsMapper
{
   /**
    * The default subject claim.
    */
   public static final String DEFAULT_SUBJECT_CLAIM = "sub";

   /**
    * The default scope claims, consulted in order.
    */
   public static final List<String> DEFAULT_SCOPE_CLAIMS = List.of(DefaultClaimsMapper.SCOPE, "scp");

   /**
    * The default roles claim.
    */
   public static final String DEFAULT_ROLES_CLAIM = "roles";

   private static final String SCOPE = "scope";
   private static final String CLAIM_PREFIX = "The JWT ";

   private final String subjectClaim;
   private final List<String> scopeClaims;
   private final String rolesClaim;

   /**
    * Constructor using the default claim names.
    */
   public DefaultClaimsMapper()
   {
      this(DEFAULT_SUBJECT_CLAIM, DEFAULT_SCOPE_CLAIMS, DEFAULT_ROLES_CLAIM);
   }

   /**
    * Constructor.
    *
    * @param subjectClaim the claim holding the subject
    * @param scopeClaims  the claims that may hold the scopes, consulted in order
    * @param rolesClaim   the claim holding the roles
    */
   public DefaultClaimsMapper(final String subjectClaim, final List<String> scopeClaims, final String rolesClaim)
   {
      this.subjectClaim = Objects.requireNonNull(subjectClaim, "subjectClaim cannot be null");
      this.scopeClaims = List.copyOf(Objects.requireNonNull(scopeClaims, "scopeClaims cannot be null"));
      this.rolesClaim = Objects.requireNonNull(rolesClaim, "rolesClaim cannot be null");
   }

   @Override
   public AuthenticatedPrincipal map(final JWTClaimsSet claims)
   {
      Object subject = claims.getClaim(subjectClaim);
      if (!(subject instanceof String text) || text.isBlank())
      {
         throw new AuthenticationException("The JWT has no " + subjectClaim + " claim identifying the caller");
      }

      Object scopes = scopeClaims.stream().map(claims::getClaim).filter(Objects::nonNull).findFirst().orElse(null);
      return new AuthenticatedPrincipal(
         text, values(scopes, SCOPE), values(claims.getClaim(rolesClaim), rolesClaim), claims.getClaims());
   }

   private static Set<String> values(final Object claim, final String name)
   {
      Set<String> values = new LinkedHashSet<>();
      switch (claim)
      {
         case null ->
         {
         }
         case String text -> Arrays.stream(text.trim().split("\\s+")).filter(v -> !v.isEmpty()).forEach(values::add);
         case Collection<?> items -> items.forEach(item -> values.add(text(item, name)));
         default -> throw new AuthenticationException(CLAIM_PREFIX + name + " claim is neither a string nor an array");
      }
      return values;
   }

   private static String text(final Object item, final String name)
   {
      if (!(item instanceof String text))
      {
         throw new AuthenticationException(CLAIM_PREFIX + name + " claim contains a value that is not a string");
      }
      return text;
   }
}
