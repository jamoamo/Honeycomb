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
package io.github.jamoamo.honeycomb.types;

/**
 * The kind of failure an {@link ErrorCode} represents.
 *
 * <p>
 * The category is the protocol-neutral classification of a failure. It exists so that an incoming boundary adapter
 * can decide how to surface a failure - an HTTP status, a GraphQL error extension, a message negative acknowledgement,
 * a retry decision - without knowing anything about the use case that produced it. The code carried alongside the
 * category remains the stable, machine readable identifier of the specific failure.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public enum ErrorCategory
{
   /**
    * The input supplied to the use case was missing, malformed or failed validation.
    */
   VALIDATION,

   /**
    * The requested entity does not exist.
    */
   NOT_FOUND,

   /**
    * The request conflicts with the current state, for example a duplicate creation.
    */
   CONFLICT,

   /**
    * The caller is not authenticated, or the credentials supplied were not accepted.
    */
   UNAUTHORISED,

   /**
    * The caller is authenticated but is not permitted to perform the operation.
    */
   FORBIDDEN,

   /**
    * A precondition on the request was not met, for example an optimistic locking version mismatch.
    */
   PRECONDITION_FAILED,

   /**
    * A dependency the use case relies on is unavailable. Typically retryable.
    */
   UNAVAILABLE,

   /**
    * An operation exceeded its time budget. Typically retryable.
    */
   TIMEOUT,

   /**
    * The caller has exceeded a rate or quota limit. Typically retryable after a delay.
    */
   RATE_LIMITED,

   /**
    * An unexpected failure that the use case did not anticipate. Never retryable without intervention.
    */
   INTERNAL
}
