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

import io.github.jamoamo.honeycomb.auth.jwt.JwtTokenAuthenticator;
import io.github.jamoamo.honeycomb.rest.auth.AuthenticatedPrincipal;
import io.github.jamoamo.honeycomb.rest.auth.TokenAuthenticator;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HoneycombJwtAutoConfiguration}.
 *
 * @author James Amoore
 */
class HoneycombJwtAutoConfigurationTest
{
   private static final String SECRET = "0123456789abcdef0123456789abcdef";

   private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(HoneycombJwtAutoConfiguration.class));

   @Test
   void registersNothingWithoutAKeySource()
   {
      runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(TokenAuthenticator.class));
   }

   @Test
   void registersAnAuthenticatorForASecret()
   {
      runner.withPropertyValues(
            "honeycomb.rest.auth.jwt.secret=" + SECRET,
            "honeycomb.rest.auth.jwt.issuer=https://issuer",
            "honeycomb.rest.auth.jwt.audiences=api",
            "honeycomb.rest.auth.jwt.roles-claim=groups")
         .run(context -> {
            TokenAuthenticator authenticator = context.getBean(TokenAuthenticator.class);
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims()
               .issuer("https://issuer").audience("api").claim("groups", List.of("admin")).build());
            jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));

            AuthenticatedPrincipal principal = authenticator.authenticate(jwt.serialize());

            assertThat(principal.subject()).isEqualTo("james");
            assertThat(principal.roles()).containsExactly("admin");
         });
   }

   @Test
   void registersAnAuthenticatorForAPublicKeyOrItsLocation(@TempDir final Path directory) throws Exception
   {
      RSAKey key = new RSAKeyGenerator(2048).generate();
      String pem = "-----BEGIN PUBLIC KEY-----\n"
         + Base64.getMimeEncoder().encodeToString(key.toPublicKey().getEncoded()) + "\n-----END PUBLIC KEY-----\n";
      Path file = Files.writeString(directory.resolve("jwt.pem"), pem);
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims().build());
      jwt.sign(new RSASSASigner(key));

      for (String property : List.of(
         "honeycomb.rest.auth.jwt.public-key=" + pem,
         "honeycomb.rest.auth.jwt.public-key-location=" + file.toUri()))
      {
         runner.withPropertyValues(property).run(context -> assertThat(
            context.getBean(TokenAuthenticator.class).authenticate(jwt.serialize()).subject()).isEqualTo("james"));
      }
   }

   @Test
   void registersAnAuthenticatorForAJwksUri()
   {
      runner.withPropertyValues("honeycomb.rest.auth.jwt.jwks-uri=https://issuer.example/.well-known/jwks.json")
         .run(context -> assertThat(context).hasSingleBean(JwtTokenAuthenticator.class));
   }

   @Test
   void failsWhenMoreThanOneKeySourceIsSet()
   {
      runner.withPropertyValues(
            "honeycomb.rest.auth.jwt.secret=" + SECRET,
            "honeycomb.rest.auth.jwt.jwks-uri=https://issuer.example/jwks")
         .run(context -> assertThat(context).hasFailed()
            .getFailure().rootCause().hasMessageContaining("Set only one of"));
   }

   @Test
   void failsWhenThePublicKeyLocationCannotBeRead()
   {
      runner.withPropertyValues("honeycomb.rest.auth.jwt.public-key-location=classpath:missing.pem")
         .run(context -> assertThat(context).hasFailed()
            .getFailure().rootCause().isInstanceOf(java.io.FileNotFoundException.class));
   }

   @Test
   void backsOffForTheApplicationsOwnAuthenticator()
   {
      TokenAuthenticator own = token -> AuthenticatedPrincipal.of("own");
      runner.withPropertyValues("honeycomb.rest.auth.jwt.secret=" + SECRET)
         .withBean(TokenAuthenticator.class, () -> own)
         .run(context -> assertThat(context.getBean(TokenAuthenticator.class)).isSameAs(own));
   }

   @Test
   void backsOffWhenAuthIsDisabled()
   {
      runner.withPropertyValues("honeycomb.rest.auth.jwt.secret=" + SECRET, "honeycomb.rest.auth.enabled=false")
         .run(context -> assertThat(context).doesNotHaveBean(TokenAuthenticator.class));
   }

   @Test
   void isRegisteredAsAnAutoConfiguration() throws Exception
   {
      ClassPathResource imports =
         new ClassPathResource("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

      assertThat(imports.getContentAsString(StandardCharsets.UTF_8).lines())
         .contains(HoneycombJwtAutoConfiguration.class.getName());
   }

   private static JWTClaimsSet.Builder claims()
   {
      return new JWTClaimsSet.Builder().subject("james").expirationTime(Date.from(Instant.now().plusSeconds(300)));
   }
}
