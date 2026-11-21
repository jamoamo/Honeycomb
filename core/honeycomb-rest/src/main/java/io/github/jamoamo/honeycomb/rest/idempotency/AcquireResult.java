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

import java.util.Objects;

/**
 * The outcome of {@link IdempotencyStore#tryAcquire}.
 *
 * @author James Amoore
 * @since 1.1.0
 */
public sealed interface AcquireResult
{
   /**
    * The key was unused (or had expired) and is now held by the caller, which must later
    * {@link IdempotencyStore#complete complete} or {@link IdempotencyStore#release release} it.
    */
   record Acquired() implements AcquireResult
   {
   }

   /**
    * The key is held by a request that has not finished yet.
    */
   record InProgress() implements AcquireResult
   {
   }

   /**
    * The key was already used for a request with a different fingerprint.
    */
   record Mismatch() implements AcquireResult
   {
   }

   /**
    * The key was already used for an identical request, which completed with the stored response.
    *
    * @param response the stored response
    */
   record Completed(StoredResponse response) implements AcquireResult
   {
      /**
       * Constructor.
       *
       * @param response the stored response
       */
      public Completed
      {
         Objects.requireNonNull(response, "response cannot be null");
      }
   }
}
