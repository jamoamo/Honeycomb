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

import io.github.jamoamo.honeycomb.rest.BadRequestException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * A dependency-free {@link ValueConverter} supporting the common scalar types: {@link String}, the primitive
 * numeric and boolean types and their wrappers, {@link UUID}, and enums.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class DefaultValueConverter implements ValueConverter
{
   private final Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

   /**
    * Constructor.
    */
   public DefaultValueConverter()
   {
      register(String.class, value -> value);
      register(int.class, Integer::valueOf);
      register(Integer.class, Integer::valueOf);
      register(long.class, Long::valueOf);
      register(Long.class, Long::valueOf);
      register(short.class, Short::valueOf);
      register(Short.class, Short::valueOf);
      register(double.class, Double::valueOf);
      register(Double.class, Double::valueOf);
      register(float.class, Float::valueOf);
      register(Float.class, Float::valueOf);
      register(boolean.class, DefaultValueConverter::parseBoolean);
      register(Boolean.class, DefaultValueConverter::parseBoolean);
      register(UUID.class, UUID::fromString);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object convert(final String value, final Class<?> targetType)
   {
      Function<String, Object> conversion = converters.get(targetType);
      try
      {
         if (conversion != null)
         {
            return conversion.apply(value);
         }
         if (targetType.isEnum())
         {
            return convertEnum(value, targetType);
         }
      }
      catch (final IllegalArgumentException ex)
      {
         throw new BadRequestException(
            "Invalid value '" + value + "' for type " + targetType.getSimpleName(), ex);
      }

      throw new BadRequestException("Unsupported parameter type: " + targetType.getName());
   }

   private void register(final Class<?> type, final Function<String, Object> conversion)
   {
      converters.put(type, conversion);
   }

   private static Object parseBoolean(final String value)
   {
      if ("true".equalsIgnoreCase(value))
      {
         return Boolean.TRUE;
      }
      if ("false".equalsIgnoreCase(value))
      {
         return Boolean.FALSE;
      }

      throw new IllegalArgumentException("Invalid boolean value: " + value);
   }

   private static Object convertEnum(final String value, final Class<?> enumType)
   {
      for (Object constant : enumType.getEnumConstants())
      {
         if (((Enum<?>) constant).name().equals(value))
         {
            return constant;
         }
      }

      throw new IllegalArgumentException("No enum constant: " + value);
   }
}
