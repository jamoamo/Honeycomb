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
package io.github.jamoamo.honeycomb.boundary.usecase;

import io.github.jamoamo.honeycomb.exception.HoneycombException;
import io.github.jamoamo.honeycomb.types.ErrorCode;

import java.util.Objects;

/**
 * Thrown by a use case, or by an outgoing boundary it calls, to abort execution with a known error code.
 *
 * <p>
 * Returning a {@link UseCaseResult.Failure} is the normal way to report a failure. This exception exists for the cases
 * where the failure is detected deep in a call chain and unwinding it by hand would obscure the code. {@link UseCase}
 * catches it and converts it into a failure carrying this error code, so the choice between the two is a matter of
 * style rather than of contract - an incoming boundary adapter sees no difference.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public class UseCaseException extends HoneycombException
{
   private static final long serialVersionUID = 1L;
   private static final String NULL_ERROR_CODE = "errorCode cannot be null";

   private final transient ErrorCode errorCode;

   /**
    * Constructor.
    *
    * @param errorCode the error code to report to the incoming boundary adapter
    */
   public UseCaseException(final ErrorCode errorCode)
   {
      super(Objects.requireNonNull(errorCode, NULL_ERROR_CODE).description());
      this.errorCode = errorCode;
   }

   /**
    * Constructor.
    *
    * @param errorCode the error code to report to the incoming boundary adapter
    * @param cause     the cause of the exception
    */
   public UseCaseException(final ErrorCode errorCode, final Throwable cause)
   {
      super(Objects.requireNonNull(errorCode, NULL_ERROR_CODE).description(), cause);
      this.errorCode = errorCode;
   }

   /**
    * The error code to report to the incoming boundary adapter.
    *
    * @return the error code
    */
   public final ErrorCode getErrorCode()
   {
      return this.errorCode;
   }
}
