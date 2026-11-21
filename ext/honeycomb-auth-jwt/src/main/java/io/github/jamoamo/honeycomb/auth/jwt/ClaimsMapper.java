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

import com.nimbusds.jwt.JWTClaimsSet;

/**
 * Maps the claims of a verified JWT to the {@link AuthenticatedPrincipal} endpoints see.
 *
 * @author James Amoore
 * @since 1.1.0
 */
@FunctionalInterface
public interface ClaimsMapper
{
   /**
    * Maps verified claims to a principal.
    *
    * @param claims the claims of a JWT whose signature and standard claims have been verified
    * @return the principal; never {@code null}
    * @throws io.github.jamoamo.honeycomb.rest.auth.AuthenticationException if the claims do not identify a caller
    */
   AuthenticatedPrincipal map(JWTClaimsSet claims);
}
