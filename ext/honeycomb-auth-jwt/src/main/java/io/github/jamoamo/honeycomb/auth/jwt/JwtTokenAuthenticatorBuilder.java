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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;

import java.net.MalformedURLException;
import java.net.URI;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.spec.SecretKeySpec;

/**
 * Builds a {@link JwtTokenAuthenticator}.
 *
 * <p>
 * Exactly one key source must be configured: a {@linkplain #jwksUri(URI) JWKS URI}, a
 * {@linkplain #publicKey(String) public key} or a {@linkplain #secret(byte[]) shared secret}. The accepted algorithms
 * default by key: {@code RS256} for a JWKS URI or an RSA key, the algorithm matching the curve of an EC key, and
 * {@code HS256} for a secret. Configured algorithms must suit the key, so an asymmetric key can never be used as an
 * HMAC secret and {@code none} is never accepted.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class JwtTokenAuthenticatorBuilder
{
   /**
    * The default allowed clock skew.
    */
   public static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(60);

   /**
    * The default time a fetched JWK set is cached.
    */
   public static final Duration DEFAULT_JWKS_CACHE_TTL = Duration.ofMinutes(5);

   /**
    * The default minimum interval between JWK set fetches.
    */
   public static final Duration DEFAULT_JWKS_MIN_REFRESH_INTERVAL = Duration.ofSeconds(30);

   private static final int MIN_RSA_KEY_BITS = 2048;
   private static final int BITS_PER_BYTE = 8;

   private URI jwksUri;
   private PublicKey publicKey;
   private byte[] secret;
   private final Set<JWSAlgorithm> algorithms = new LinkedHashSet<>();
   private String issuer;
   private final Set<String> audiences = new LinkedHashSet<>();
   private Duration clockSkew = DEFAULT_CLOCK_SKEW;
   private Duration jwksCacheTtl = DEFAULT_JWKS_CACHE_TTL;
   private Duration jwksMinRefreshInterval = DEFAULT_JWKS_MIN_REFRESH_INTERVAL;
   private ClaimsMapper claimsMapper = new DefaultClaimsMapper();

   JwtTokenAuthenticatorBuilder()
   {
   }

   /**
    * Verifies tokens with the keys published at a JWKS URI, fetched on demand, cached and refreshed when a token
    * names an unknown {@code kid}.
    *
    * @param uri the JWKS URI
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder jwksUri(final URI uri)
   {
      this.jwksUri = Objects.requireNonNull(uri, "uri cannot be null");
      return this;
   }

   /**
    * Verifies tokens with an RSA or EC public key, given as a PEM {@code PUBLIC KEY} or {@code CERTIFICATE} block.
    *
    * @param pem the PEM text
    * @return this builder
    * @throws IllegalArgumentException if the text holds no supported key
    */
   public JwtTokenAuthenticatorBuilder publicKey(final String pem)
   {
      return publicKey(PemKeys.parsePublicKey(Objects.requireNonNull(pem, "pem cannot be null")));
   }

   /**
    * Verifies tokens with an RSA or EC public key.
    *
    * @param key the public key
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder publicKey(final PublicKey key)
   {
      this.publicKey = Objects.requireNonNull(key, "key cannot be null");
      return this;
   }

   /**
    * Verifies HMAC-signed tokens with a shared secret, which must be at least as long as each accepted algorithm
    * requires: 256 bits for {@code HS256}.
    *
    * @param sharedSecret the secret
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder secret(final byte[] sharedSecret)
   {
      this.secret = Objects.requireNonNull(sharedSecret, "sharedSecret cannot be null").clone();
      return this;
   }

   /**
    * The accepted signature algorithms, replacing the default for the key source.
    *
    * @param names the JOSE algorithm names, for example {@code RS256}
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder algorithms(final Collection<String> names)
   {
      algorithms.clear();
      names.forEach(name -> algorithms.add(JWSAlgorithm.parse(Objects.requireNonNull(name, "name cannot be null"))));
      return this;
   }

   /**
    * The accepted signature algorithms, replacing the default for the key source.
    *
    * @param names the JOSE algorithm names, for example {@code RS256}
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder algorithms(final String... names)
   {
      return algorithms(Arrays.asList(names));
   }

   /**
    * Requires the {@code iss} claim to equal the issuer.
    *
    * @param expectedIssuer the issuer
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder issuer(final String expectedIssuer)
   {
      this.issuer = Objects.requireNonNull(expectedIssuer, "expectedIssuer cannot be null");
      return this;
   }

   /**
    * Requires the {@code aud} claim to name at least one of the audiences.
    *
    * @param accepted the accepted audiences
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder audiences(final Collection<String> accepted)
   {
      audiences.clear();
      accepted.forEach(audience -> audiences.add(Objects.requireNonNull(audience, "audience cannot be null")));
      return this;
   }

   /**
    * Requires the {@code aud} claim to name at least one of the audiences.
    *
    * @param accepted the accepted audiences
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder audiences(final String... accepted)
   {
      return audiences(Arrays.asList(accepted));
   }

   /**
    * The clock skew allowed when checking {@code exp} and {@code nbf}; 60 seconds by default.
    *
    * @param skew the allowed skew
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder clockSkew(final Duration skew)
   {
      this.clockSkew = nonNegative(skew, "skew");
      return this;
   }

   /**
    * How long a fetched JWK set is cached; 5 minutes by default.
    *
    * @param ttl the cache time-to-live
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder jwksCacheTtl(final Duration ttl)
   {
      this.jwksCacheTtl = nonNegative(ttl, "ttl");
      return this;
   }

   /**
    * The minimum interval between JWK set fetches, which bounds how often tokens naming unknown keys can make the
    * authenticator call the JWKS endpoint; 30 seconds by default, and zero to disable the limit.
    *
    * @param interval the minimum interval
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder jwksMinRefreshInterval(final Duration interval)
   {
      this.jwksMinRefreshInterval = nonNegative(interval, "interval");
      return this;
   }

   /**
    * The mapper from verified claims to the principal; a {@link DefaultClaimsMapper} by default.
    *
    * @param mapper the claims mapper
    * @return this builder
    */
   public JwtTokenAuthenticatorBuilder claimsMapper(final ClaimsMapper mapper)
   {
      this.claimsMapper = Objects.requireNonNull(mapper, "mapper cannot be null");
      return this;
   }

   /**
    * Builds the authenticator.
    *
    * @return the authenticator
    * @throws IllegalStateException    if not exactly one key source is configured
    * @throws IllegalArgumentException if the key or algorithms are unsuitable
    */
   public JwtTokenAuthenticator build()
   {
      long sources = Stream.of(jwksUri, publicKey, secret).filter(Objects::nonNull).count();
      if (sources != 1)
      {
         throw new IllegalStateException("Exactly one of a JWKS URI, a public key or a secret must be configured, "
            + "but " + sources + " were");
      }
      JwtKeySource keySource;
      if (jwksUri != null)
      {
         keySource = jwksSource();
      }
      else if (publicKey != null)
      {
         keySource = publicKeySource();
      }
      else
      {
         keySource = secretSource();
      }
      return new JwtTokenAuthenticator(this, keySource);
   }

   String configuredIssuer()
   {
      return issuer;
   }

   Set<String> configuredAudiences()
   {
      return Set.copyOf(audiences);
   }

   Duration configuredClockSkew()
   {
      return clockSkew;
   }

   ClaimsMapper configuredClaimsMapper()
   {
      return claimsMapper;
   }

   private JwtKeySource jwksSource()
   {
      Set<JWSAlgorithm> allowed = new LinkedHashSet<>(JWSAlgorithm.Family.RSA);
      allowed.addAll(JWSAlgorithm.Family.EC);
      Set<JWSAlgorithm> accepted = accepted(allowed, JWSAlgorithm.RS256, "a JWKS URI");
      if (!jwksMinRefreshInterval.isZero() && jwksMinRefreshInterval.compareTo(jwksCacheTtl) >= 0)
      {
         throw new IllegalArgumentException("The JWKS minimum refresh interval (" + jwksMinRefreshInterval
            + ") must be less than the JWKS cache time-to-live (" + jwksCacheTtl + ")");
      }
      try
      {
         JWKSourceBuilder<SecurityContext> source = JWKSourceBuilder.<SecurityContext>create(jwksUri.toURL())
            .cache(jwksCacheTtl.toMillis(), JWKSourceBuilder.DEFAULT_CACHE_REFRESH_TIMEOUT);
         source = jwksMinRefreshInterval.isZero()
            ? source.rateLimited(false)
            : source.rateLimited(jwksMinRefreshInterval.toMillis());
         return new JwtKeySource.Jwks(source.build(), accepted);
      }
      catch (final MalformedURLException | IllegalArgumentException ex)
      {
         throw new IllegalArgumentException("Invalid JWKS configuration for " + jwksUri + ": " + ex.getMessage(), ex);
      }
   }

   private JwtKeySource publicKeySource()
   {
      return switch (publicKey)
      {
         case RSAPublicKey rsa ->
         {
            if (rsa.getModulus().bitLength() < MIN_RSA_KEY_BITS)
            {
               throw new IllegalArgumentException("RSA keys must be at least " + MIN_RSA_KEY_BITS + " bits");
            }
            yield new JwtKeySource.Static(rsa, accepted(JWSAlgorithm.Family.RSA, JWSAlgorithm.RS256, "an RSA key"));
         }
         case ECPublicKey ec ->
         {
            Curve curve = Curve.forECParameterSpec(ec.getParams());
            Set<JWSAlgorithm> allowed = JWSAlgorithm.Family.EC.stream()
               .filter(algorithm -> curve != null && Curve.forJWSAlgorithm(algorithm).contains(curve))
               .collect(Collectors.toCollection(LinkedHashSet::new));
            if (allowed.isEmpty())
            {
               throw new IllegalArgumentException("The EC key uses an unsupported curve");
            }
            yield new JwtKeySource.Static(ec, accepted(allowed, allowed.iterator().next(), "an EC key on " + curve));
         }
         default -> throw new IllegalArgumentException(
            "Unsupported public key type " + publicKey.getAlgorithm() + "; expected RSA or EC");
      };
   }

   private JwtKeySource secretSource()
   {
      Set<JWSAlgorithm> accepted = accepted(JWSAlgorithm.Family.HMAC_SHA, JWSAlgorithm.HS256, "a secret");
      for (JWSAlgorithm algorithm : accepted)
      {
         int required = minSecretBits(algorithm);
         if (secret.length * BITS_PER_BYTE < required)
         {
            throw new IllegalArgumentException("The secret must be at least " + required + " bits for " + algorithm);
         }
      }
      return new JwtKeySource.Static(new SecretKeySpec(secret, "HmacSHA256"), accepted);
   }

   private Set<JWSAlgorithm> accepted(
      final Set<JWSAlgorithm> allowed,
      final JWSAlgorithm defaultAlgorithm,
      final String keyDescription)
   {
      Set<JWSAlgorithm> accepted = algorithms.isEmpty() ? Set.of(defaultAlgorithm) : Set.copyOf(algorithms);
      for (JWSAlgorithm algorithm : accepted)
      {
         if (!allowed.contains(algorithm))
         {
            throw new IllegalArgumentException(
               "Algorithm " + algorithm + " cannot be used with " + keyDescription + "; allowed: " + allowed);
         }
      }
      return accepted;
   }

   private static int minSecretBits(final JWSAlgorithm algorithm)
   {
      try
      {
         return MACSigner.getMinRequiredSecretLength(algorithm);
      }
      catch (final JOSEException ex)
      {
         throw new IllegalArgumentException("Unsupported HMAC algorithm " + algorithm, ex);
      }
   }

   private static Duration nonNegative(final Duration duration, final String name)
   {
      Objects.requireNonNull(duration, name + " cannot be null");
      if (duration.isNegative())
      {
         throw new IllegalArgumentException(name + " cannot be negative");
      }
      return duration;
   }
}
