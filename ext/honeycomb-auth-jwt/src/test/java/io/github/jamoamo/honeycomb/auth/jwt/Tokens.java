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
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.PublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Key and token fixtures for the JWT tests.
 *
 * @author James Amoore
 */
final class Tokens
{
   static final byte[] SECRET = "0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8);

   private Tokens()
   {
   }

   static RSAKey rsa(final String keyId)
   {
      try
      {
         return new RSAKeyGenerator(2048).keyID(keyId).generate();
      }
      catch (final JOSEException ex)
      {
         throw new IllegalStateException(ex);
      }
   }

   static ECKey ec(final Curve curve)
   {
      try
      {
         return new ECKeyGenerator(curve).keyID("ec").generate();
      }
      catch (final JOSEException ex)
      {
         throw new IllegalStateException(ex);
      }
   }

   static JWTClaimsSet.Builder claims()
   {
      return new JWTClaimsSet.Builder()
         .subject("james")
         .expirationTime(Date.from(Instant.now().plusSeconds(300)));
   }

   static String sign(final RSAKey key, final JWSAlgorithm algorithm, final JWTClaimsSet claims)
   {
      try
      {
         return sign(new RSASSASigner(key), new JWSHeader.Builder(algorithm).keyID(key.getKeyID()).build(), claims);
      }
      catch (final JOSEException ex)
      {
         throw new IllegalStateException(ex);
      }
   }

   static String sign(final RSAKey key, final JWTClaimsSet claims)
   {
      return sign(key, JWSAlgorithm.RS256, claims);
   }

   static String sign(final ECKey key, final JWSAlgorithm algorithm, final JWTClaimsSet claims)
   {
      try
      {
         return sign(new ECDSASigner(key), new JWSHeader.Builder(algorithm).keyID(key.getKeyID()).build(), claims);
      }
      catch (final JOSEException ex)
      {
         throw new IllegalStateException(ex);
      }
   }

   static String signHmac(final byte[] secret, final JWSAlgorithm algorithm, final JWTClaimsSet claims)
   {
      try
      {
         return sign(new MACSigner(secret), new JWSHeader(algorithm), claims);
      }
      catch (final JOSEException ex)
      {
         throw new IllegalStateException(ex);
      }
   }

   static String pem(final PublicKey key)
   {
      return "-----BEGIN PUBLIC KEY-----\n"
         + Base64.getMimeEncoder(64, "\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
            .encodeToString(key.getEncoded())
         + "\n-----END PUBLIC KEY-----\n";
   }

   private static String sign(final JWSSigner signer, final JWSHeader header, final JWTClaimsSet claims)
      throws JOSEException
   {
      SignedJWT jwt = new SignedJWT(header, claims);
      jwt.sign(signer);
      return jwt.serialize();
   }
}
