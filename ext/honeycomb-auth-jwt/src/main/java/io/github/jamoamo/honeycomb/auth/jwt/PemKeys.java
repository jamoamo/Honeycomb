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

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses PEM-encoded public keys without any additional security provider.
 *
 * @author James Amoore
 */
final class PemKeys
{
   private static final Pattern PEM = Pattern.compile(
      "-----BEGIN ([A-Z ]+)-----([A-Za-z0-9+/=\\s]+)-----END \\1-----");
   private static final List<String> KEY_ALGORITHMS = List.of("RSA", "EC");

   private PemKeys()
   {
   }

   /**
    * Parses an RSA or EC public key from a {@code PUBLIC KEY} (SubjectPublicKeyInfo) or {@code CERTIFICATE} PEM block.
    *
    * @param pem the PEM text
    * @return the public key
    * @throws IllegalArgumentException if the text holds no supported key
    */
   static PublicKey parsePublicKey(final String pem)
   {
      Matcher matcher = PEM.matcher(pem);
      if (!matcher.find())
      {
         throw new IllegalArgumentException("No PEM block found; expected a PUBLIC KEY or CERTIFICATE");
      }
      byte[] der = Base64.getMimeDecoder().decode(matcher.group(2));
      return switch (matcher.group(1))
      {
         case "PUBLIC KEY" -> keyFromSpki(der);
         case "CERTIFICATE" -> keyFromCertificate(der);
         default -> throw new IllegalArgumentException(
            "Unsupported PEM block " + matcher.group(1) + "; expected a PUBLIC KEY or CERTIFICATE");
      };
   }

   private static PublicKey keyFromSpki(final byte[] der)
   {
      for (String algorithm : KEY_ALGORITHMS)
      {
         try
         {
            return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(der));
         }
         catch (final GeneralSecurityException ex)
         {
            // Not a key of this algorithm; try the next.
         }
      }
      throw new IllegalArgumentException("The PUBLIC KEY is neither an RSA nor an EC key");
   }

   private static PublicKey keyFromCertificate(final byte[] der)
   {
      try
      {
         return CertificateFactory.getInstance("X.509")
            .generateCertificate(new ByteArrayInputStream(der))
            .getPublicKey();
      }
      catch (final GeneralSecurityException ex)
      {
         throw new IllegalArgumentException("The CERTIFICATE could not be parsed", ex);
      }
   }
}
