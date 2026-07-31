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
package io.github.jamoamo.honeycomb.rest;

/**
 * Thrown when a request cannot be bound to a handler method, for example when a required query parameter is
 * missing or a value cannot be converted to the target type, resulting in a {@code 400 Bad Request} response.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public class BadRequestException extends RestException
{
   private static final long serialVersionUID = 1L;

   /**
    * Constructor.
    *
    * @param message the exception message
    */
   public BadRequestException(final String message)
   {
      super(message);
   }

   /**
    * Constructor.
    *
    * @param message the exception message
    * @param cause   the underlying cause of the exception
    */
   public BadRequestException(final String message, final Throwable cause)
   {
      super(message, cause);
   }
}
