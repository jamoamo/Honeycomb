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
import io.github.jamoamo.honeycomb.messaging.adapter.SubjectToken;
import io.github.jamoamo.honeycomb.messaging.message.ConsumedMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SubjectTokenArgumentResolver} and {@link ConsumedMessageArgumentResolver}.
 *
 * @author James Amoore
 */
@DisplayName("SubjectTokenArgumentResolver")
public class SubjectTokenArgumentResolverTest
{
   public static final class Handlers
   {
      public void named(@SubjectToken("orderId") final String orderId)
      {
         // handled
      }

      public void byParameterName(@SubjectToken final String orderId)
      {
         // handled
      }

      public void converted(@SubjectToken("orderId") final int orderId)
      {
         // handled
      }

      public void message(final ConsumedMessage message)
      {
         // handled
      }

      public void unannotated(final String payload)
      {
         // handled
      }
   }

   private final SubjectTokenArgumentResolver resolver =
      new SubjectTokenArgumentResolver(new DefaultValueConverter());
   private final ConsumedMessageArgumentResolver messageResolver = new ConsumedMessageArgumentResolver();

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

   private static MessageResolutionContext context(final Map<String, String> tokens)
   {
      return new MessageResolutionContext(new FakeConsumedMessage("orders.42.created"), tokens);
   }

   @Test
   @DisplayName("supports only annotated parameters")
   public void testSupports()
   {
      assertThat(resolver.supports(parameterOf("named"))).isTrue();
      assertThat(resolver.supports(parameterOf("unannotated"))).isFalse();
   }

   @Test
   @DisplayName("resolves a captured token")
   public void testResolvesToken()
   {
      Object resolved = resolver.resolve(parameterOf("named"), context(Map.of("orderId", "42")));

      assertThat(resolved).isEqualTo("42");
   }

   @Test
   @DisplayName("uses the parameter name when the annotation names no token")
   public void testUsesParameterName()
   {
      Object resolved = resolver.resolve(parameterOf("byParameterName"), context(Map.of("orderId", "42")));

      assertThat(resolved).isEqualTo("42");
   }

   @Test
   @DisplayName("converts a token to the parameter type")
   public void testConvertsToken()
   {
      Object resolved = resolver.resolve(parameterOf("converted"), context(Map.of("orderId", "42")));

      assertThat(resolved).isEqualTo(42);
   }

   @Test
   @DisplayName("reports an unconvertible token as unbindable")
   public void testRejectsUnconvertibleToken()
   {
      MessageResolutionContext context = context(Map.of("orderId", "forty-two"));

      assertThatExceptionOfType(MalformedMessageException.class)
         .isThrownBy(() -> resolver.resolve(parameterOf("converted"), context))
         .withMessageContaining("Invalid value");
   }

   @Test
   @DisplayName("reports an uncaptured token as unbindable")
   public void testRejectsMissingToken()
   {
      MessageResolutionContext context = context(Map.of());

      assertThatExceptionOfType(MalformedMessageException.class)
         .isThrownBy(() -> resolver.resolve(parameterOf("named"), context))
         .withMessageContaining("Missing subject token");
   }

   @Test
   @DisplayName("resolves a message-typed parameter to the message being handled")
   public void testResolvesMessage()
   {
      MessageResolutionContext context = context(Map.of());

      assertThat(messageResolver.supports(parameterOf("message"))).isTrue();
      assertThat(messageResolver.supports(parameterOf("named"))).isFalse();
      assertThat(messageResolver.resolve(parameterOf("message"), context))
         .isSameAs(context.message());
   }
}
