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
package io.github.jamoamo.honeycomb.usecase;

import io.github.jamoamo.honeycomb.types.ErrorCode;
import io.github.jamoamo.honeycomb.usecase.validation.ValidationError;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The outcome of executing a use case: either a success carrying the output, or a failure carrying an error code.
 *
 * <p>
 * A use case never signals failure to an incoming boundary adapter by throwing. Every outcome, expected or not, is
 * returned as one of the two variants of this type, so an adapter can exhaustively handle both.
 * </p>
 *
 * @author James Amoore
 * @param <O> the type of the use case output
 * @since 1.0.0
 */
public sealed interface UseCaseResult<O>
{
   /**
    * A successful outcome.
    *
    * @author James Amoore
    * @param <O>    the type of the use case output
    * @param output the output of the use case, null for a use case that produces no output
    * @since 1.0.0
    */
   record Success<O>(O output) implements UseCaseResult<O>
   {
      /**
       * A success never carries validation errors.
       *
       * @return an empty list
       */
      @Override
      public List<ValidationError> validationErrors()
      {
         return List.of();
      }
   }

   /**
    * A failed outcome.
    *
    * @author James Amoore
    * @param <O>              the type of the use case output
    * @param errorCode        the error code identifying the failure
    * @param validationErrors the individual input problems, empty unless the failure was a validation failure
    * @since 1.0.0
    */
   record Failure<O>(ErrorCode errorCode, List<ValidationError> validationErrors) implements UseCaseResult<O>
   {
      /**
       * Constructor.
       *
       * @param errorCode        the error code identifying the failure
       * @param validationErrors the individual input problems, empty unless the failure was a validation failure
       */
      public Failure(final ErrorCode errorCode, final List<ValidationError> validationErrors)
      {
         this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
         this.validationErrors =
            List.copyOf(Objects.requireNonNull(validationErrors, "validationErrors cannot be null"));
      }
   }

   /**
    * A successful outcome carrying the supplied output.
    *
    * @param output the output of the use case
    * @param <O>    the type of the use case output
    * @return a successful result
    */
   static <O> UseCaseResult<O> success(final O output)
   {
      return new Success<>(output);
   }

   /**
    * A successful outcome for a use case that produces no output.
    *
    * @return a successful result with no output
    */
   static UseCaseResult<Void> success()
   {
      return new Success<>(null);
   }

   /**
    * A failed outcome.
    *
    * @param error the error code identifying the failure
    * @param <O>   the type of the use case output
    * @return a failed result
    */
   static <O> UseCaseResult<O> failure(final ErrorCode error)
   {
      return new Failure<>(error, List.of());
   }

   /**
    * A failed outcome carrying the individual input problems that caused it.
    *
    * @param error            the error code identifying the failure
    * @param validationErrors the individual input problems
    * @param <O>              the type of the use case output
    * @return a failed result
    */
   static <O> UseCaseResult<O> failure(final ErrorCode error, final List<ValidationError> validationErrors)
   {
      return new Failure<>(error, validationErrors);
   }

   /**
    * Whether the use case succeeded.
    *
    * @return true if this is a success
    */
   default boolean isSuccess()
   {
      return this instanceof Success;
   }

   /**
    * Whether the use case failed.
    *
    * @return true if this is a failure
    */
   default boolean isFailure()
   {
      return this instanceof Failure;
   }

   /**
    * The output of the use case.
    *
    * @return the output, empty on failure or when the use case produces no output
    */
   default Optional<O> value()
   {
      if(this instanceof Success<O> success)
      {
         return Optional.ofNullable(success.output());
      }
      return Optional.empty();
   }

   /**
    * The error code identifying the failure.
    *
    * @return the error code, empty on success
    */
   default Optional<ErrorCode> error()
   {
      if(this instanceof Failure<O> failure)
      {
         return Optional.of(failure.errorCode());
      }
      return Optional.empty();
   }

   /**
    * The individual input problems that caused the failure.
    *
    * @return the validation errors, empty on success and on failures that are not validation failures
    */
   List<ValidationError> validationErrors();

   /**
    * Applies the supplied function to the output, leaving a failure untouched.
    *
    * @param mapper the function to apply to the output
    * @param <R>    the type the output is mapped to
    * @return a success carrying the mapped output, or this failure
    */
   default <R> UseCaseResult<R> map(final Function<? super O, ? extends R> mapper)
   {
      requireMapper(mapper);
      if(this instanceof Success<O> success)
      {
         return new Success<>(mapper.apply(success.output()));
      }
      Failure<O> failure = (Failure<O>) this;
      return new Failure<>(failure.errorCode(), failure.validationErrors());
   }

   /**
    * Applies the supplied result returning function to the output, leaving a failure untouched.
    *
    * @param mapper the function to apply to the output
    * @param <R>    the type the output is mapped to
    * @return the result of the function, or this failure
    */
   default <R> UseCaseResult<R> flatMap(final Function<? super O, UseCaseResult<R>> mapper)
   {
      requireMapper(mapper);
      if(this instanceof Success<O> success)
      {
         return Objects.requireNonNull(mapper.apply(success.output()), "mapper cannot return null");
      }
      Failure<O> failure = (Failure<O>) this;
      return new Failure<>(failure.errorCode(), failure.validationErrors());
   }

   /**
    * Collapses both variants into a single value, the shape an incoming boundary adapter normally wants.
    *
    * @param onSuccess applied to the output when the use case succeeded
    * @param onFailure applied to the failure when the use case failed
    * @param <R>       the type both variants are collapsed to
    * @return the value produced by whichever function was applied
    */
   default <R> R fold(
      final Function<? super O, ? extends R> onSuccess,
      final Function<? super Failure<O>, ? extends R> onFailure)
   {
      Objects.requireNonNull(onSuccess, "onSuccess cannot be null");
      Objects.requireNonNull(onFailure, "onFailure cannot be null");
      if(this instanceof Success<O> success)
      {
         return onSuccess.apply(success.output());
      }
      return onFailure.apply((Failure<O>) this);
   }

   private static <T> void requireMapper(final T mapper)
   {
      Objects.requireNonNull(mapper, "mapper cannot be null");
   }
}
