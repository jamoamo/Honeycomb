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

import io.github.jamoamo.honeycomb.boundary.validation.ValidationResult;
import io.github.jamoamo.honeycomb.types.ErrorCategory;
import io.github.jamoamo.honeycomb.types.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for use cases.
 *
 * <p>
 * A use case handles a single business capability. It is called by an incoming boundary adapter - a REST controller,
 * a GraphQL resolver, a scheduled task, an event consumer - and reaches everything outside itself through outgoing
 * boundary ports. It knows nothing about the adapter that called it or the adapters behind its ports.
 * </p>
 *
 * <p>
 * Subclasses implement {@link #executeUseCase(Object)} and, where the input needs checking, override
 * {@link #validate(Object)}. Callers invoke {@link #execute(Object)}, which is final and guarantees three things:
 * </p>
 *
 * <ul>
 *    <li>the input is non-null and has passed {@link #validate(Object)} before {@code executeUseCase} is reached;</li>
 *    <li>nothing is thrown - every outcome, including an unanticipated one, is returned as a
 *        {@link UseCaseResult};</li>
 *    <li>every failure carries an {@link ErrorCode} whose {@link ErrorCategory} lets the calling adapter decide how to
 *        surface it without knowing anything about this use case.</li>
 * </ul>
 *
 * @author James Amoore
 * @param <I> the type of the use case input
 * @param <O> the type of the use case output
 * @since 1.0.0
 */
public abstract class UseCase<I, O>
{
   /**
    * The code of the failure returned when {@code null} input is supplied to a use case.
    */
   public static final String NULL_INPUT = "NULL_INPUT";

   /**
    * The code of the failure returned when the input fails {@link #validate(Object)}.
    */
   public static final String VALIDATION_FAILED = "VALIDATION_FAILED";

   /**
    * The code of the failure returned when a use case throws something it did not anticipate.
    */
   public static final String UNHANDLED_ERROR = "UNHANDLED_ERROR";

   private static final Logger LOGGER = LoggerFactory.getLogger(UseCase.class);
   private static final String DESCRIPTION_PREFIX = "Use case (";

   /**
    * Executes the use case.
    *
    * <p>
    * This method does not throw. A failure of any kind, anticipated or not, is returned as a
    * {@link UseCaseResult.Failure}.
    * </p>
    *
    * @param input the input to the use case
    * @return the outcome of the use case
    */
   @SuppressWarnings("checkstyle:IllegalCatch")
   public final UseCaseResult<O> execute(final I input)
   {
      LOGGER.debug("Executing use case {} with input: {}", useCaseName(), input);
      try
      {
         return validateAndExecute(input);
      }
      catch (final UseCaseException e)
      {
         LOGGER.warn("Use case {} aborted with error code {}", useCaseName(), e.getErrorCode().code(), e);
         return UseCaseResult.failure(e.getErrorCode());
      }
      catch (final Throwable e)
      {
         LOGGER.error("Use case {} failed unexpectedly with input: {}", useCaseName(), input, e);
         return UseCaseResult.failure(unhandledError(e));
      }
   }

   /**
    * Validates the input before the use case is executed.
    *
    * <p>
    * The default implementation accepts any non-null input. Override to check the input; returning an invalid result
    * short circuits execution with a {@link #VALIDATION_FAILED} failure carrying the individual errors, and
    * {@link #executeUseCase(Object)} is never reached.
    * </p>
    *
    * @param input the input to the use case, never null
    * @return the outcome of validating the input
    */
   protected ValidationResult validate(final I input)
   {
      return ValidationResult.valid();
   }

   /**
    * Executes the business capability.
    *
    * <p>
    * Called only with input that is non-null and has passed {@link #validate(Object)}. Implementations should return a
    * failure for anticipated failures, or throw a {@link UseCaseException} where returning one would obscure the code.
    * Anything else thrown is reported as an {@link #UNHANDLED_ERROR} failure.
    * </p>
    *
    * @param input the input to the use case
    * @return the outcome of the use case
    */
   protected abstract UseCaseResult<O> executeUseCase(I input);

   private UseCaseResult<O> validateAndExecute(final I input)
   {
      if(input == null)
      {
         LOGGER.warn("Use case {} was called with null input", useCaseName());
         return UseCaseResult.failure(nullInputError());
      }

      ValidationResult validation = validate(input);
      if(!validation.isValid())
      {
         LOGGER.debug("Use case {} rejected its input: {}", useCaseName(), validation.errors());
         return UseCaseResult.failure(validationFailedError(), validation.errors());
      }

      return executeUseCase(input);
   }

   private ErrorCode nullInputError()
   {
      return new ErrorCode(
         NULL_INPUT,
         ErrorCategory.VALIDATION,
         DESCRIPTION_PREFIX + useCaseName() + ") requires an input.");
   }

   private ErrorCode validationFailedError()
   {
      return new ErrorCode(
         VALIDATION_FAILED,
         ErrorCategory.VALIDATION,
         DESCRIPTION_PREFIX + useCaseName() + ") rejected the supplied input.");
   }

   private ErrorCode unhandledError(final Throwable cause)
   {
      return new ErrorCode(
         UNHANDLED_ERROR,
         ErrorCategory.INTERNAL,
         DESCRIPTION_PREFIX + useCaseName() + ") failed to handle error: " + cause.getMessage());
   }

   private String useCaseName()
   {
      return this.getClass().getSimpleName();
   }
}
