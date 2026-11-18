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
package io.github.jamoamo.honeycomb.rest.argument;

import io.github.jamoamo.honeycomb.adapter.argument.ValueConversionException;
import io.github.jamoamo.honeycomb.adapter.argument.ValueConverter;
import io.github.jamoamo.honeycomb.rest.BadRequestException;

/**
 * Decorates a {@link ValueConverter} so that a protocol-neutral conversion failure surfaces as the REST
 * failure for an unbindable request.
 *
 * @author James Amoore
 * @since 1.0.0
 */
final class BadRequestValueConverter implements ValueConverter
{
   private final ValueConverter delegate;

   /**
    * Constructor.
    *
    * @param delegate the converter performing the conversion
    */
   BadRequestValueConverter(final ValueConverter delegate)
   {
      this.delegate = delegate;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object convert(final String value, final Class<?> targetType)
   {
      return convert(value, targetType, null);
   }

   /**
    * Converts the value of the named request parameter. The client-facing message only names the parameter;
    * the conversion failure, which may describe internal types or echo the input, is kept as the cause.
    *
    * @param value      the raw value
    * @param targetType the type to convert to
    * @param name       the name of the parameter being converted, or {@code null} when unknown
    * @return the converted value
    * @throws BadRequestException if the value cannot be converted
    */
   Object convert(final String value, final Class<?> targetType, final String name)
   {
      try
      {
         return delegate.convert(value, targetType);
      }
      catch (final ValueConversionException ex)
      {
         String message = name == null ? "Invalid parameter value." : "Invalid value for parameter '" + name + "'.";
         throw new BadRequestException(message, ex);
      }
   }
}
