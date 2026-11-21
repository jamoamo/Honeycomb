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
package io.github.jamoamo.honeycomb.auth.jwt.autoconfigure;

import io.github.jamoamo.honeycomb.auth.jwt.DefaultClaimsMapper;
import io.github.jamoamo.honeycomb.auth.jwt.JwtTokenAuthenticator;
import io.github.jamoamo.honeycomb.auth.jwt.JwtTokenAuthenticatorBuilder;
import io.github.jamoamo.honeycomb.rest.auth.TokenAuthenticator;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

/**
 * Auto-configuration contributing a {@link JwtTokenAuthenticator} as the application's {@link TokenAuthenticator}
 * when a JWT key source is configured under {@code honeycomb.rest.auth.jwt}.
 *
 * <p>
 * It applies only while REST authentication is enabled and backs off when the application declares its own
 * {@link TokenAuthenticator}.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
@AutoConfiguration
@ConditionalOnBooleanProperty(prefix = "honeycomb.rest.auth", name = "enabled", matchIfMissing = true)
@Conditional(HoneycombJwtAutoConfiguration.OnJwtKeySource.class)
@EnableConfigurationProperties(HoneycombJwtProperties.class)
public class HoneycombJwtAutoConfiguration
{
   /**
    * The JWT authenticator.
    *
    * @param properties the JWT properties
    * @return the authenticator
    * @throws IllegalStateException if more than one key source is configured
    */
   @Bean
   @ConditionalOnMissingBean(TokenAuthenticator.class)
   public JwtTokenAuthenticator honeycombJwtTokenAuthenticator(final HoneycombJwtProperties properties)
   {
      long sources = Stream.of(
            properties.jwksUri(), properties.publicKey(), properties.publicKeyLocation(), properties.secret())
         .filter(source -> source != null)
         .count();
      if (sources > 1)
      {
         throw new IllegalStateException("Set only one of " + HoneycombJwtProperties.PREFIX + ".jwks-uri, "
            + "public-key, public-key-location or secret; " + sources + " are set");
      }

      JwtTokenAuthenticatorBuilder builder = JwtTokenAuthenticator.builder()
         .audiences(properties.audiences())
         .algorithms(properties.algorithms())
         .clockSkew(properties.clockSkew())
         .jwksCacheTtl(properties.jwksCacheTtl())
         .jwksMinRefreshInterval(properties.jwksMinRefreshInterval())
         .claimsMapper(new DefaultClaimsMapper(
            properties.subjectClaim(), properties.scopeClaims(), properties.rolesClaim()));
      if (properties.issuer() != null)
      {
         builder.issuer(properties.issuer());
      }
      return keySource(builder, properties).build();
   }

   private static JwtTokenAuthenticatorBuilder keySource(
      final JwtTokenAuthenticatorBuilder builder,
      final HoneycombJwtProperties properties)
   {
      if (properties.jwksUri() != null)
      {
         return builder.jwksUri(properties.jwksUri());
      }
      if (properties.secret() != null)
      {
         return builder.secret(properties.secret().getBytes(StandardCharsets.UTF_8));
      }
      return builder.publicKey(properties.publicKey() != null ? properties.publicKey() : read(properties));
   }

   private static String read(final HoneycombJwtProperties properties)
   {
      try
      {
         return properties.publicKeyLocation().getContentAsString(StandardCharsets.US_ASCII);
      }
      catch (final IOException ex)
      {
         throw new UncheckedIOException("Unable to read " + properties.publicKeyLocation(), ex);
      }
   }

   /**
    * Matches when any JWT key source property is set.
    */
   static final class OnJwtKeySource extends SpringBootCondition
   {
      private static final List<String> KEY_SOURCES =
         List.of("jwks-uri", "public-key", "public-key-location", "secret");

      @Override
      public ConditionOutcome getMatchOutcome(final ConditionContext context, final AnnotatedTypeMetadata metadata)
      {
         boolean configured = KEY_SOURCES.stream()
            .anyMatch(name -> context.getEnvironment().containsProperty(HoneycombJwtProperties.PREFIX + "." + name));
         return configured
            ? ConditionOutcome.match("a JWT key source is configured")
            : ConditionOutcome.noMatch("no " + HoneycombJwtProperties.PREFIX + " key source is configured");
      }
   }
}
