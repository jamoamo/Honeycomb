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
import io.github.jamoamo.honeycomb.rest.FakeRestRequest;
import io.github.jamoamo.honeycomb.rest.adapter.PathVariable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link PathVariableArgumentResolver}.
 *
 * @author James Amoore
 */
@DisplayName("PathVariableArgumentResolver")
public class PathVariableArgumentResolverTest
{
   private final PathVariableArgumentResolver resolver =
      new PathVariableArgumentResolver(new DefaultValueConverter());

   @SuppressWarnings("unused")
   private static final class Endpoints
   {
      public void named(@PathVariable("userId") final int id)
      {
      }

      public void unnamed(@PathVariable final String name)
      {
      }

      public void plain(final String body)
      {
      }
   }

   @Test
   @DisplayName("supports only annotated parameters")
   public void testSupports()
   {
      assertThat(resolver.supports(parameter("named"))).isTrue();
      assertThat(resolver.supports(parameter("plain"))).isFalse();
   }

   @Test
   @DisplayName("resolves a variable by its declared name")
   public void testResolvesByDeclaredName()
   {
      Object value = resolver.resolve(parameter("named"), context(Map.of("userId", "42")));

      assertThat(value).isEqualTo(42);
   }

   @Test
   @DisplayName("falls back to the parameter name when no name is declared")
   public void testResolvesByParameterName()
   {
      Object value = resolver.resolve(parameter("unnamed"), context(Map.of("name", "james")));

      assertThat(value).isEqualTo("james");
   }

   @Test
   @DisplayName("rejects a request without the declared variable")
   public void testRejectsMissingVariable()
   {
      assertThatExceptionOfType(BadRequestException.class)
         .isThrownBy(() -> resolver.resolve(parameter("named"), context(Map.of())))
         .withMessageContaining("Missing path variable");
   }

   private static ArgumentResolutionContext context(final Map<String, String> pathVariables)
   {
      return new ArgumentResolutionContext(new FakeRestRequest("GET", "/"), pathVariables);
   }

   private static Parameter parameter(final String methodName)
   {
      for (Method method : Endpoints.class.getDeclaredMethods())
      {
         if (method.getName().equals(methodName))
         {
            return method.getParameters()[0];
         }
      }

      throw new AssertionError("No such method: " + methodName);
   }
}
