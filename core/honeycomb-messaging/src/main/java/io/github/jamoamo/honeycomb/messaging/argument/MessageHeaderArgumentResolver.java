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
package io.github.jamoamo.honeycomb.messaging.argument;

import io.github.jamoamo.honeycomb.adapter.argument.AdapterArgumentResolver;
import io.github.jamoamo.honeycomb.adapter.argument.ValueConverter;
import io.github.jamoamo.honeycomb.messaging.MalformedMessageException;
import io.github.jamoamo.honeycomb.messaging.adapter.MessageHeader;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves a parameter annotated with {@link MessageHeader} from the headers of the message being handled.
 *
 * <p>
 * A parameter typed as {@link Optional} yields an empty optional when the header is absent, and a parameter
 * typed as {@link List} collects all values supplied for the name. For a plain-typed parameter the declared
 * {@link MessageHeader#defaultValue()} is applied when the value is absent; a missing value for a required
 * parameter without a default makes the message unbindable.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class MessageHeaderArgumentResolver implements AdapterArgumentResolver<MessageResolutionContext>
{
   private final ValueConverter valueConverter;

   /**
    * Constructor.
    *
    * @param valueConverter the converter used to coerce raw values to the parameter type
    */
   public MessageHeaderArgumentResolver(final ValueConverter valueConverter)
   {
      this.valueConverter = new MalformedMessageValueConverter(valueConverter);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean supports(final Parameter parameter)
   {
      return parameter.isAnnotationPresent(MessageHeader.class);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object resolve(final Parameter parameter, final MessageResolutionContext context)
   {
      MessageHeader annotation = parameter.getAnnotation(MessageHeader.class);
      String name = annotation.value().isEmpty() ? parameter.getName() : annotation.value();

      if (parameter.getType() == Optional.class)
      {
         return resolveOptional(parameter, context, name);
      }
      if (parameter.getType() == List.class)
      {
         return resolveList(parameter, context, name);
      }

      return resolveScalar(parameter, annotation, context, name);
   }

   private Object resolveScalar(
      final Parameter parameter,
      final MessageHeader annotation,
      final MessageResolutionContext context,
      final String name)
   {
      String rawValue = singleValue(context, name);
      if (rawValue == null)
      {
         rawValue = applyDefault(annotation, name);
      }
      if (rawValue == null)
      {
         if (parameter.getType().isPrimitive())
         {
            throw missingRequired(name);
         }
         return null;
      }

      return valueConverter.convert(rawValue, parameter.getType());
   }

   private Object resolveOptional(
      final Parameter parameter, final MessageResolutionContext context, final String name)
   {
      String rawValue = singleValue(context, name);
      if (rawValue == null)
      {
         return Optional.empty();
      }

      return Optional.of(valueConverter.convert(rawValue, elementType(parameter)));
   }

   private Object resolveList(
      final Parameter parameter, final MessageResolutionContext context, final String name)
   {
      List<String> rawValues = context.message().headerValues(name);
      List<Object> values = new ArrayList<>();
      Class<?> elementType = elementType(parameter);
      for (String rawValue : rawValues)
      {
         values.add(valueConverter.convert(rawValue, elementType));
      }

      return values;
   }

   private static String applyDefault(final MessageHeader annotation, final String name)
   {
      if (!MessageHeader.NO_DEFAULT.equals(annotation.defaultValue()))
      {
         return annotation.defaultValue();
      }
      if (annotation.required())
      {
         throw missingRequired(name);
      }

      return null;
   }

   private static MalformedMessageException missingRequired(final String name)
   {
      return new MalformedMessageException("Missing required message header " + quotedName(name));
   }

   private static String singleValue(final MessageResolutionContext context, final String name)
   {
      List<String> values = context.message().headerValues(name);
      if (values.isEmpty())
      {
         return null;
      }
      if (values.size() > 1)
      {
         throw new MalformedMessageException("Multiple values supplied for message header " + quotedName(name));
      }

      return values.get(0);
   }

   private static String quotedName(final String name)
   {
      return "'" + name + "'.";
   }

   private static Class<?> elementType(final Parameter parameter)
   {
      Type parameterizedType = parameter.getParameterizedType();
      if (parameterizedType instanceof ParameterizedType generic
         && generic.getActualTypeArguments()[0] instanceof Class<?> elementType)
      {
         return elementType;
      }

      return String.class;
   }
}
