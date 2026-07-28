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
package io.github.jamoamo.honeycomb.exception;

/**
 * Base class for all exceptions thrown by Honeycomb libraries.
 *
 * <p>
 * Honeycomb exceptions are unchecked. Errors crossing a boundary are represented as data rather than as exceptions,
 * so an exception reaching an incoming boundary adapter always indicates an unhandled condition.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public abstract class HoneycombException extends RuntimeException
{
   private static final long serialVersionUID = 1L;

   /**
    * Constructor.
    *
    * @param message the detail message
    */
   protected HoneycombException(final String message)
   {
      super(message);
   }

   /**
    * Constructor.
    *
    * @param cause the cause of the exception
    */
   protected HoneycombException(final Throwable cause)
   {
      super(cause);
   }

   /**
    * Constructor.
    *
    * @param message the detail message
    * @param cause   the cause of the exception
    */
   protected HoneycombException(final String message, final Throwable cause)
   {
      super(message, cause);
   }
}
