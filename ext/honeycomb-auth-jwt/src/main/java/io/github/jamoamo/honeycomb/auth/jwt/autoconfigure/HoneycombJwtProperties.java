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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for JWT authentication, bound from {@code honeycomb.rest.auth.jwt}.
 *
 * <p>
 * Exactly one key source must be set: {@code jwks-uri}, {@code public-key}, {@code public-key-location} or
 * {@code secret}.
 * </p>
 *
 * @author James Amoore
 * @param jwksUri                the URI of the JWK set used to verify tokens
 * @param publicKey              a PEM {@code PUBLIC KEY} or {@code CERTIFICATE} used to verify tokens
 * @param publicKeyLocation      a resource holding such a PEM block, for example {@code file:/etc/keys/jwt.pem}
 * @param secret                 a shared HMAC secret, used as its UTF-8 bytes
 * @param issuer                 the required {@code iss}, or {@code null} to accept any issuer
 * @param audiences              the accepted audiences; empty to accept any audience
 * @param algorithms             the accepted algorithms; empty for the default of the key source
 * @param clockSkew              the clock skew allowed when checking {@code exp} and {@code nbf}
 * @param subjectClaim           the claim holding the subject
 * @param scopeClaims            the claims that may hold the scopes, consulted in order
 * @param rolesClaim             the claim holding the roles
 * @param jwksCacheTtl           how long a fetched JWK set is cached
 * @param jwksMinRefreshInterval the minimum interval between JWK set fetches; zero disables the limit
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = HoneycombJwtProperties.PREFIX)
@SuppressWarnings("checkstyle:RecordComponentNumber")
public record HoneycombJwtProperties(
   URI jwksUri,
   String publicKey,
   Resource publicKeyLocation,
   String secret,
   String issuer,
   @DefaultValue List<String> audiences,
   @DefaultValue List<String> algorithms,
   @DefaultValue("60s") Duration clockSkew,
   @DefaultValue(DefaultClaimsMapper.DEFAULT_SUBJECT_CLAIM) String subjectClaim,
   @DefaultValue({"scope", "scp"}) List<String> scopeClaims,
   @DefaultValue(DefaultClaimsMapper.DEFAULT_ROLES_CLAIM) String rolesClaim,
   @DefaultValue("5m") Duration jwksCacheTtl,
   @DefaultValue("30s") Duration jwksMinRefreshInterval)
{
   /**
    * The property prefix.
    */
   public static final String PREFIX = "honeycomb.rest.auth.jwt";
}
