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
package io.github.jamoamo.honeycomb.messaging;

import io.github.jamoamo.honeycomb.exception.HoneycombException;

/**
 * An exception thrown during message consumer adapter handling.
 *
 * <p>
 * A failure of this kind is assumed to be transient: the message is left unacknowledged so that the broker
 * redelivers it.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public class MessagingException extends HoneycombException
{
   private static final long serialVersionUID = 1L;

   /**
    * Constructor.
    *
    * @param message the exception message
    */
   public MessagingException(final String message)
   {
      super(message);
   }

   /**
    * Constructor.
    *
    * @param message the exception message
    * @param cause   the underlying cause of the exception
    */
   public MessagingException(final String message, final Throwable cause)
   {
      super(message, cause);
   }
}
