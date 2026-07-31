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

import io.github.jamoamo.honeycomb.adapter.argument.DefaultValueConverter;
import io.github.jamoamo.honeycomb.messaging.FakeConsumedMessage;
import io.github.jamoamo.honeycomb.messaging.MalformedMessageException;
import io.github.jamoamo.honeycomb.messaging.adapter.MessageHeader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MessageHeaderArgumentResolver}.
 *
 * @author James Amoore
 */
@DisplayName("MessageHeaderArgumentResolver")
public class MessageHeaderArgumentResolverTest
{
   public static final class Handlers
   {
      public void required(@MessageHeader("carrier") final String carrier)
      {
         // handled
      }

      public void primitive(@MessageHeader("attempts") final int attempts)
      {
         // handled
      }

      public void primitiveNotRequired(
         @MessageHeader(value = "attempts", required = false) final int attempts)
      {
         // handled
      }

      public void defaulted(
         @MessageHeader(value = "carrier", defaultValue = "DHL") final String carrier)
      {
         // handled
      }

      public void notRequired(
         @MessageHeader(value = "carrier", required = false) final String carrier)
      {
         // handled
      }

      public void optional(@MessageHeader("attempts") final Optional<Integer> attempts)
      {
         // handled
      }

      public void all(@MessageHeader("tag") final List<String> tags)
      {
         // handled
      }

      public void raw(@MessageHeader("tag") final List tags)
      {
         // handled
      }

      public void named(@MessageHeader final String carrier)
      {
         // handled
      }

      public void unannotated(final String payload)
      {
         // handled
      }
   }

   private final MessageHeaderArgumentResolver resolver =
      new MessageHeaderArgumentResolver(new DefaultValueConverter());

   private static Parameter parameterOf(final String methodName)
   {
      for (Method method : Handlers.class.getMethods())
      {
         if (method.getName().equals(methodName))
         {
            return method.getParameters()[0];
         }
      }

      throw new IllegalArgumentException("No such handler: " + methodName);
   }

   private static MessageResolutionContext context(final Map<String, List<String>> headers)
   {
      return new MessageResolutionContext(
         new FakeConsumedMessage("orders.created", headers, "", 1L), Map.of());
   }

   @Test
   @DisplayName("supports only annotated parameters")
   public void testSupports()
   {
      assertThat(resolver.supports(parameterOf("required"))).isTrue();
      assertThat(resolver.supports(parameterOf("unannotated"))).isFalse();
   }

   @Test
   @DisplayName("resolves a header value")
   public void testResolvesValue()
   {
      Object resolved =
         resolver.resolve(parameterOf("required"), context(Map.of("carrier", List.of("DHL"))));

      assertThat(resolved).isEqualTo("DHL");
   }

   @Test
   @DisplayName("uses the parameter name when the annotation names no header")
   public void testUsesParameterName()
   {
      Object resolved =
         resolver.resolve(parameterOf("named"), context(Map.of("carrier", List.of("DHL"))));

      assertThat(resolved).isEqualTo("DHL");
   }

   @Test
   @DisplayName("converts a header value to the parameter type")
   public void testConvertsValue()
   {
      Object resolved =
         resolver.resolve(parameterOf("primitive"), context(Map.of("attempts", List.of("2"))));

      assertThat(resolved).isEqualTo(2);
   }

   @Test
   @DisplayName("reports an unconvertible header value as unbindable")
   public void testRejectsUnconvertibleValue()
   {
      MessageResolutionContext context = context(Map.of("attempts", List.of("many")));

      assertThatExceptionOfType(MalformedMessageException.class)
         .isThrownBy(() -> resolver.resolve(parameterOf("primitive"), context))
         .withMessageContaining("Invalid value");
   }

   @Test
   @DisplayName("applies the declared default when the header is absent")
   public void testAppliesDefault()
   {
      assertThat(resolver.resolve(parameterOf("defaulted"), context(Map.of()))).isEqualTo("DHL");
   }

   @Test
   @DisplayName("resolves an optional parameter to empty when the header is absent")
   public void testResolvesEmptyOptional()
   {
      assertThat(resolver.resolve(parameterOf("optional"), context(Map.of()))).isEqualTo(Optional.empty());
   }

   @Test
   @DisplayName("resolves an optional parameter to the converted value")
   public void testResolvesPresentOptional()
   {
      Object resolved =
         resolver.resolve(parameterOf("optional"), context(Map.of("attempts", List.of("2"))));

      assertThat(resolved).isEqualTo(Optional.of(2));
   }

   @Test
   @DisplayName("collects every value supplied for a list parameter")
   public void testResolvesList()
   {
      Object resolved =
         resolver.resolve(parameterOf("all"), context(Map.of("tag", List.of("a", "b"))));

      assertThat(resolved).isEqualTo(List.of("a", "b"));
   }

   @Test
   @DisplayName("treats the elements of a raw list parameter as strings")
   public void testResolvesRawList()
   {
      Object resolved =
         resolver.resolve(parameterOf("raw"), context(Map.of("tag", List.of("a"))));

      assertThat(resolved).isEqualTo(List.of("a"));
   }

   @Test
   @DisplayName("resolves an optional parameter of a non-required header to null")
   public void testResolvesAbsentNotRequired()
   {
      assertThat(resolver.resolve(parameterOf("notRequired"), context(Map.of()))).isNull();
   }

   @Test
   @DisplayName("reports an absent required header as unbindable")
   public void testRejectsAbsentRequiredHeader()
   {
      MessageResolutionContext context = context(Map.of());

      assertThatExceptionOfType(MalformedMessageException.class)
         .isThrownBy(() -> resolver.resolve(parameterOf("required"), context))
         .withMessageContaining("Missing required message header");
   }

   @Test
   @DisplayName("reports an absent header for a primitive parameter as unbindable")
   public void testRejectsAbsentPrimitiveHeader()
   {
      MessageResolutionContext context = context(Map.of());

      assertThatExceptionOfType(MalformedMessageException.class)
         .isThrownBy(() -> resolver.resolve(parameterOf("primitiveNotRequired"), context))
         .withMessageContaining("Missing required message header");
   }

   @Test
   @DisplayName("reports several values for a single-valued header as unbindable")
   public void testRejectsMultipleValues()
   {
      MessageResolutionContext context = context(Map.of("carrier", List.of("DHL", "UPS")));

      assertThatExceptionOfType(MalformedMessageException.class)
         .isThrownBy(() -> resolver.resolve(parameterOf("required"), context))
         .withMessageContaining("Multiple values supplied");
   }
}
