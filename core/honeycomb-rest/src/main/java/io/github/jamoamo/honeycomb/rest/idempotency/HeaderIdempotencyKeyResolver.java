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
package io.github.jamoamo.honeycomb.rest.idempotency;

import io.github.jamoamo.honeycomb.rest.request.RestRequest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves the idempotency key from a request header, {@code X-Idempotency-Key} by default. A blank header counts
 * as no key.
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class HeaderIdempotencyKeyResolver implements IdempotencyKeyResolver
{
   /**
    * The default header name.
    */
   public static final String DEFAULT_HEADER = "X-Idempotency-Key";

   private final String header;

   /**
    * Constructor using the {@value #DEFAULT_HEADER} header.
    */
   public HeaderIdempotencyKeyResolver()
   {
      this(DEFAULT_HEADER);
   }

   /**
    * Constructor.
    *
    * @param header the header carrying the key, matched case-insensitively
    */
   public HeaderIdempotencyKeyResolver(final String header)
   {
      this.header = Objects.requireNonNull(header, "header cannot be null");
   }

   @Override
   public Optional<String> resolve(final RestRequest request)
   {
      List<String> values = request.headerValues(header);
      return values.isEmpty() || values.getFirst().isBlank()
         ? Optional.empty()
         : Optional.of(values.getFirst().strip());
   }
}
