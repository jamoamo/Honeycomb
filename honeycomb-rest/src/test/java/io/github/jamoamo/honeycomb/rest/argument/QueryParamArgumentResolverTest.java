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
import io.github.jamoamo.honeycomb.rest.adapter.QueryParam;

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
 * Tests for {@link QueryParamArgumentResolver}.
 *
 * @author James Amoore
 */
@DisplayName("QueryParamArgumentResolver")
public class QueryParamArgumentResolverTest
{
   private final QueryParamArgumentResolver resolver =
      new QueryParamArgumentResolver(new DefaultValueConverter());

   @SuppressWarnings("unused")
   private static final class Endpoints
   {
      public void named(@QueryParam("page") final int page)
      {
      }

      public void unnamed(@QueryParam final String sort)
      {
      }

      public void defaulted(@QueryParam(value = "size", defaultValue = "20") final int size)
      {
      }

      public void notRequired(@QueryParam(value = "filter", required = false) final String filter)
      {
      }

      public void notRequiredPrimitive(@QueryParam(value = "limit", required = false) final int limit)
      {
      }

      public void optional(@QueryParam("cursor") final Optional<Integer> cursor)
      {
      }

      public void list(@QueryParam("tag") final List<Integer> tags)
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
   @DisplayName("resolves a scalar by its declared name")
   public void testResolvesScalar()
   {
      Object value = resolver.resolve(parameter("named"), context(Map.of("page", List.of("3"))));

      assertThat(value).isEqualTo(3);
   }

   @Test
   @DisplayName("falls back to the parameter name when no name is declared")
   public void testResolvesByParameterName()
   {
      Object value = resolver.resolve(parameter("unnamed"), context(Map.of("sort", List.of("name"))));

      assertThat(value).isEqualTo("name");
   }

   @Test
   @DisplayName("applies the declared default when the parameter is absent")
   public void testAppliesDefault()
   {
      Object value = resolver.resolve(parameter("defaulted"), context(Map.of()));

      assertThat(value).isEqualTo(20);
   }

   @Test
   @DisplayName("rejects a missing required parameter")
   public void testRejectsMissingRequired()
   {
      assertThatExceptionOfType(BadRequestException.class)
         .isThrownBy(() -> resolver.resolve(parameter("named"), context(Map.of())))
         .withMessageContaining("Missing required query parameter");
   }

   @Test
   @DisplayName("resolves a missing non-required parameter to null")
   public void testMissingNotRequired()
   {
      assertThat(resolver.resolve(parameter("notRequired"), context(Map.of()))).isNull();
   }

   @Test
   @DisplayName("rejects a missing non-required primitive parameter")
   public void testMissingNotRequiredPrimitive()
   {
      assertThatExceptionOfType(BadRequestException.class)
         .isThrownBy(() -> resolver.resolve(parameter("notRequiredPrimitive"), context(Map.of())));
   }

   @Test
   @DisplayName("rejects multiple values for a scalar parameter")
   public void testRejectsMultipleValues()
   {
      assertThatExceptionOfType(BadRequestException.class)
         .isThrownBy(() -> resolver.resolve(parameter("named"), context(Map.of("page", List.of("1", "2")))))
         .withMessageContaining("Multiple values");
   }

   @Test
   @DisplayName("resolves an Optional parameter")
   public void testResolvesOptional()
   {
      assertThat(resolver.resolve(parameter("optional"), context(Map.of("cursor", List.of("7")))))
         .isEqualTo(Optional.of(7));
      assertThat(resolver.resolve(parameter("optional"), context(Map.of())))
         .isEqualTo(Optional.empty());
   }

   @Test
   @DisplayName("resolves a List parameter collecting every value")
   public void testResolvesList()
   {
      assertThat(resolver.resolve(parameter("list"), context(Map.of("tag", List.of("1", "2")))))
         .isEqualTo(List.of(1, 2));
      assertThat(resolver.resolve(parameter("list"), context(Map.of()))).isEqualTo(List.of());
   }

   private static ArgumentResolutionContext context(final Map<String, List<String>> queryParameters)
   {
      return new ArgumentResolutionContext(
         new FakeRestRequest("GET", "/", queryParameters, ""), Map.of());
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
