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
package io.github.jamoamo.honeycomb.adapter.argument;

import io.github.jamoamo.honeycomb.exception.HoneycombException;

/**
 * Thrown by a {@link ValueConverter} when a raw value cannot be converted to the type expected by a handler
 * method.
 *
 * <p>
 * Conversion failures are protocol-neutral, so each adapter translates this into the failure its protocol
 * understands - a bad-request response for REST, an unprocessable message for messaging.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public class ValueConversionException extends HoneycombException
{
   private static final long serialVersionUID = 1L;

   /**
    * Constructor.
    *
    * @param message the exception message
    */
   public ValueConversionException(final String message)
   {
      super(message);
   }

   /**
    * Constructor.
    *
    * @param message the exception message
    * @param cause   the underlying cause of the exception
    */
   public ValueConversionException(final String message, final Throwable cause)
   {
      super(message, cause);
   }
}
