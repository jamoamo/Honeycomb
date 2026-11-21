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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;

import java.security.Key;
import java.util.List;
import java.util.Set;

/**
 * Where the keys that verify JWT signatures come from, together with the algorithms they may be used with.
 *
 * @author James Amoore
 */
sealed interface JwtKeySource
{
   /**
    * The selector the JWT processor uses to find verification keys.
    *
    * @return the key selector
    */
   JWSKeySelector<SecurityContext> keySelector();

   /**
    * Keys published as a JWK set, selected by the token's {@code kid} and algorithm.
    *
    * @param source     the JWK source
    * @param algorithms the accepted algorithms
    */
   record Jwks(JWKSource<SecurityContext> source, Set<JWSAlgorithm> algorithms) implements JwtKeySource
   {
      @Override
      public JWSKeySelector<SecurityContext> keySelector()
      {
         return new JWSVerificationKeySelector<>(algorithms, source);
      }
   }

   /**
    * A single configured key, used for every token signed with an accepted algorithm whatever its {@code kid}.
    *
    * @param key        the verification key
    * @param algorithms the accepted algorithms
    */
   record Static(Key key, Set<JWSAlgorithm> algorithms) implements JwtKeySource
   {
      @Override
      public JWSKeySelector<SecurityContext> keySelector()
      {
         return (header, context) -> algorithms.contains(header.getAlgorithm()) ? List.of(key) : List.of();
      }
   }
}
