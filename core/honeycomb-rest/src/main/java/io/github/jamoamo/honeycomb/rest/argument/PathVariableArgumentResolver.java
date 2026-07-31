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

import io.github.jamoamo.honeycomb.adapter.argument.AdapterArgumentResolver;
import io.github.jamoamo.honeycomb.adapter.argument.ValueConverter;
import io.github.jamoamo.honeycomb.rest.BadRequestException;
import io.github.jamoamo.honeycomb.rest.adapter.PathVariable;

import java.lang.reflect.Parameter;

/**
 * Resolves a parameter annotated with {@link PathVariable} from the variables captured during route matching.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class PathVariableArgumentResolver implements AdapterArgumentResolver<ArgumentResolutionContext>
{
   private final ValueConverter valueConverter;

   /**
    * Constructor.
    *
    * @param valueConverter the converter used to coerce the raw value to the parameter type
    */
   public PathVariableArgumentResolver(final ValueConverter valueConverter)
   {
      this.valueConverter = new BadRequestValueConverter(valueConverter);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean supports(final Parameter parameter)
   {
      return parameter.isAnnotationPresent(PathVariable.class);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object resolve(final Parameter parameter, final ArgumentResolutionContext context)
   {
      PathVariable annotation = parameter.getAnnotation(PathVariable.class);
      String name = annotation.value().isEmpty() ? parameter.getName() : annotation.value();

      String rawValue = context.pathVariables().get(name);
      if (rawValue == null)
      {
         throw new BadRequestException("Missing path variable '" + name + "'.");
      }

      return valueConverter.convert(rawValue, parameter.getType());
   }
}
