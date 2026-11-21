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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A response kept by an {@link IdempotencyStore} for replay, with its body already serialized so that any store can
 * persist it as plain bytes.
 *
 * @author James Amoore
 * @param status  the HTTP status
 * @param headers the response headers
 * @param body    the serialized JSON body, or an empty array when the response had none
 * @since 1.1.0
 */
public record StoredResponse(int status, Map<String, List<String>> headers, byte[] body)
{
   /**
    * Constructor.
    *
    * @param status  the HTTP status
    * @param headers the response headers
    * @param body    the serialized JSON body, or an empty array when the response had none
    */
   public StoredResponse
   {
      headers = Map.copyOf(Objects.requireNonNull(headers, "headers cannot be null"));
      body = Objects.requireNonNull(body, "body cannot be null").clone();
   }

   /**
    * The serialized body.
    *
    * @return a copy of the body bytes
    */
   @Override
   public byte[] body()
   {
      return body.clone();
   }

   @Override
   public boolean equals(final Object other)
   {
      return other instanceof StoredResponse that
         && status == that.status
         && headers.equals(that.headers)
         && Arrays.equals(body, that.body);
   }

   @Override
   public int hashCode()
   {
      return Objects.hash(status, headers, Arrays.hashCode(body));
   }

   @Override
   public String toString()
   {
      return "StoredResponse[status=" + status + ", headers=" + headers + ", body=" + body.length + " bytes]";
   }
}
