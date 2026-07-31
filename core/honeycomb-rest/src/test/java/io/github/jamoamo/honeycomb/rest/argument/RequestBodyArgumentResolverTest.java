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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link RequestBodyArgumentResolver}.
 *
 * @author James Amoore
 */
@DisplayName("RequestBodyArgumentResolver")
public class RequestBodyArgumentResolverTest
{
   private final RequestBodyArgumentResolver resolver =
      new RequestBodyArgumentResolver(JsonMapper.builder().build());

   public record Body(String name, int age)
   {
   }

   @SuppressWarnings("unused")
   private static final class Endpoints
   {
      public void create(final Body body)
      {
      }
   }

   @Test
   @DisplayName("supports any parameter as the fallback resolver")
   public void testSupportsAnyParameter()
   {
      assertThat(resolver.supports(parameter())).isTrue();
   }

   @Test
   @DisplayName("deserializes the request body into the parameter type")
   public void testDeserializesBody() throws IOException
   {
      ArgumentResolutionContext context = context("{\"name\":\"james\",\"age\":42}");

      Object value = resolver.resolve(parameter(), context);

      assertThat(value).isEqualTo(new Body("james", 42));
   }

   @Test
   @DisplayName("rejects a malformed request body")
   public void testRejectsMalformedBody()
   {
      ArgumentResolutionContext context = context("{not-json");

      assertThatExceptionOfType(BadRequestException.class)
         .isThrownBy(() -> resolver.resolve(parameter(), context))
         .withMessageContaining("Malformed request body");
   }

   private static ArgumentResolutionContext context(final String body)
   {
      return new ArgumentResolutionContext(
         new FakeRestRequest("POST", "/", Map.of(), body), Map.of());
   }

   private static Parameter parameter()
   {
      for (Method method : Endpoints.class.getDeclaredMethods())
      {
         if (method.getName().equals("create"))
         {
            return method.getParameters()[0];
         }
      }

      throw new AssertionError("No create method");
   }
}
