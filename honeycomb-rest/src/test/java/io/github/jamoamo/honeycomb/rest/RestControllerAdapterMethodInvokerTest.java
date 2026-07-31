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
package io.github.jamoamo.honeycomb.rest;

import io.github.jamoamo.honeycomb.rest.adapter.PathVariable;
import io.github.jamoamo.honeycomb.rest.adapter.QueryParam;
import io.github.jamoamo.honeycomb.rest.request.RestRequest;
import io.github.jamoamo.honeycomb.rest.response.ApiResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link RestControllerAdapterMethodInvoker}.
 *
 * @author James Amoore
 */
@DisplayName("RestControllerAdapterMethodInvoker")
public class RestControllerAdapterMethodInvokerTest
{
   private final RestControllerAdapterMethodInvoker invoker =
      new RestControllerAdapterMethodInvoker(JsonMapper.builder().build());

   public record Body(String name)
   {
   }

   public static final class Adapter
   {
      public String greet(@PathVariable("id") final int id, @QueryParam("greeting") final String greeting)
      {
         return greeting + " " + id;
      }

      public Body echo(final Body body)
      {
         return body;
      }

      public String nothing()
      {
         return null;
      }

      public String boom()
      {
         throw new IllegalStateException("kaboom");
      }
   }

   @Test
   @DisplayName("binds path variables and query parameters and wraps the result")
   public void testBindsAnnotatedParameters()
   {
      RouteMatch match = match("greet", Map.of("id", "42"));
      RestRequest request = new FakeRestRequest(
         "GET", "/greet/42", Map.of("greeting", List.of("hello")), "");

      ApiResponse<?> response = invoker.invokeRestControllerAdapterMethod(match, request);

      assertThat(response.data().orElseThrow()).isEqualTo("hello 42");
      assertThat(response.meta()).isEmpty();
   }

   @Test
   @DisplayName("binds the request body through the fallback resolver")
   public void testBindsRequestBody()
   {
      RouteMatch match = match("echo", Map.of());
      RestRequest request = new FakeRestRequest("POST", "/echo", Map.of(), "{\"name\":\"james\"}");

      ApiResponse<?> response = invoker.invokeRestControllerAdapterMethod(match, request);

      assertThat(response.data().orElseThrow()).isEqualTo(new Body("james"));
   }

   @Test
   @DisplayName("wraps a null result as an empty response")
   public void testWrapsNullResult()
   {
      ApiResponse<?> response = invoker.invokeRestControllerAdapterMethod(
         match("nothing", Map.of()), new FakeRestRequest("GET", "/nothing"));

      assertThat(response.data()).isEmpty();
   }

   @Test
   @DisplayName("surfaces a handler exception as a RestException carrying the cause")
   public void testHandlerExceptionSurfacesAsRestException()
   {
      assertThatExceptionOfType(RestException.class)
         .isThrownBy(() -> invoker.invokeRestControllerAdapterMethod(
            match("boom", Map.of()), new FakeRestRequest("GET", "/boom")))
         .withCauseInstanceOf(IllegalStateException.class);
   }

   @Test
   @DisplayName("surfaces a request read failure as a RestException")
   public void testBodyReadFailureSurfacesAsRestException()
   {
      RestRequest failing = new RestRequest()
      {
         @Override
         public String method()
         {
            return "POST";
         }

         @Override
         public String path()
         {
            return "/echo";
         }

         @Override
         public List<String> queryParameterValues(final String name)
         {
            return List.of();
         }

         @Override
         public InputStream body() throws IOException
         {
            throw new IOException("connection reset");
         }
      };

      assertThatExceptionOfType(RestException.class)
         .isThrownBy(() -> invoker.invokeRestControllerAdapterMethod(match("echo", Map.of()), failing))
         .withCauseInstanceOf(IOException.class);
   }

   @Test
   @DisplayName("fails when no resolver supports a parameter")
   public void testNoResolverSupportsParameter()
   {
      RestControllerAdapterMethodInvoker noResolvers = new RestControllerAdapterMethodInvoker(List.of());

      assertThatIllegalStateException()
         .isThrownBy(() -> noResolvers.invokeRestControllerAdapterMethod(
            match("echo", Map.of()), new FakeRestRequest("POST", "/echo")))
         .withMessageContaining("No argument resolver");
   }

   @Test
   @DisplayName("invokes a parameterless method with custom resolvers")
   public void testParameterlessMethodWithCustomResolvers()
   {
      RestControllerAdapterMethodInvoker noResolvers = new RestControllerAdapterMethodInvoker(List.of());

      ApiResponse<?> response = noResolvers.invokeRestControllerAdapterMethod(
         match("nothing", Map.of()), new FakeRestRequest("GET", "/nothing"));

      assertThat(response.data()).isEmpty();
   }

   private static RouteMatch match(final String methodName, final Map<String, String> pathVariables)
   {
      for (Method method : Adapter.class.getMethods())
      {
         if (method.getName().equals(methodName))
         {
            return new RouteMatch(new HandlerMethod(new Adapter(), method), pathVariables);
         }
      }

      throw new AssertionError("No such method: " + methodName);
   }

   @Test
   @DisplayName("wraps a data value via the Optional-based response factory")
   public void testResponseFactory()
   {
      ApiResponse<String> response = ApiResponse.of("value");

      assertThat(response.data()).contains("value");
      assertThat(response.meta()).isEmpty();
   }
}
