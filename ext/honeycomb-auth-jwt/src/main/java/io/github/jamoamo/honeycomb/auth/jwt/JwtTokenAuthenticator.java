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
import io.github.jamoamo.honeycomb.rest.auth.TokenAuthenticator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link TokenAuthenticator} that accepts signed JWTs, backed by Nimbus JOSE+JWT.
 *
 * <p>
 * A token is accepted only when it is signed with one of the configured algorithms by a configured key, carries an
 * {@code exp} claim that has not passed, any {@code nbf} claim has been reached (both allowing for the configured
 * clock skew), and, when configured, its {@code iss} matches the issuer and its {@code aud} names one of the
 * audiences. Unsigned and encrypted tokens are always rejected. Accepted claims are mapped to an
 * {@link AuthenticatedPrincipal} by a {@link ClaimsMapper}.
 * </p>
 *
 * <p>
 * A token that fails any check is rejected with an {@link AuthenticationException}. A failure to retrieve the
 * verification keys, such as an unreachable JWKS endpoint, is a server-side failure and surfaces as an
 * {@link IllegalStateException} instead, so that it is answered with a {@code 500} rather than blamed on the caller.
 * </p>
 *
 * <p>
 * Instances are built with {@link #builder()} and are thread safe.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class JwtTokenAuthenticator implements TokenAuthenticator
{
   private final ConfigurableJWTProcessor<SecurityContext> processor;
   private final ClaimsMapper claimsMapper;

   JwtTokenAuthenticator(final JwtTokenAuthenticatorBuilder builder, final JwtKeySource keySource)
   {
      DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
      jwtProcessor.setJWSKeySelector(keySource.keySelector());

      Set<String> required = new HashSet<>(Set.of("exp"));
      JWTClaimsSet.Builder exact = new JWTClaimsSet.Builder();
      if (builder.configuredIssuer() != null)
      {
         exact.issuer(builder.configuredIssuer());
         required.add("iss");
      }
      Set<String> audiences = builder.configuredAudiences().isEmpty() ? null : builder.configuredAudiences();
      if (audiences != null)
      {
         required.add("aud");
      }
      DefaultJWTClaimsVerifier<SecurityContext> verifier =
         new DefaultJWTClaimsVerifier<>(audiences, exact.build(), required, null);
      verifier.setMaxClockSkew(Math.toIntExact(builder.configuredClockSkew().toSeconds()));
      jwtProcessor.setJWTClaimsSetVerifier(verifier);

      this.processor = jwtProcessor;
      this.claimsMapper = builder.configuredClaimsMapper();
   }

   /**
    * Starts building an authenticator.
    *
    * @return a new builder
    */
   public static JwtTokenAuthenticatorBuilder builder()
   {
      return new JwtTokenAuthenticatorBuilder();
   }

   @Override
   public AuthenticatedPrincipal authenticate(final String token)
   {
      JWTClaimsSet claims;
      try
      {
         claims = processor.process(token, null);
      }
      catch (final ParseException | BadJOSEException ex)
      {
         throw new AuthenticationException("The JWT was rejected: " + ex.getMessage(), ex);
      }
      catch (final KeySourceException ex)
      {
         throw new IllegalStateException("The JWT verification keys could not be retrieved", ex);
      }
      catch (final JOSEException ex)
      {
         throw new AuthenticationException("The JWT could not be verified: " + ex.getMessage(), ex);
      }
      return Objects.requireNonNull(claimsMapper.map(claims), "ClaimsMapper returned a null principal");
   }
}
