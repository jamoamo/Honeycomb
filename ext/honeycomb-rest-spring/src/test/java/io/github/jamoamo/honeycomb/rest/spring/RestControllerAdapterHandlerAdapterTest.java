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
package io.github.jamoamo.honeycomb.rest.spring;

import io.github.jamoamo.honeycomb.rest.HandlerMethod;
import io.github.jamoamo.honeycomb.rest.RestControllerAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.rest.RouteMatch;
import io.github.jamoamo.honeycomb.rest.adapter.QueryParam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RestControllerAdapterHandlerAdapter}.
 *
 * @author James Amoore
 */
@DisplayName("RestControllerAdapterHandlerAdapter")
public class RestControllerAdapterHandlerAdapterTest
{
   private final RestControllerAdapterHandlerAdapter adapter = new RestControllerAdapterHandlerAdapter(
      new RestControllerAdapterMethodInvoker(JsonMapper.builder().build()),
      JsonMapper.builder().build());

   public static final class TestAdapter
   {
      public String hello(@QueryParam("name") final String name)
      {
         return "hello " + name;
      }

      public String boom()
      {
         throw new IllegalStateException("kaboom");
      }
   }

   @Test
   @DisplayName("supports only RouteMatch handlers")
   public void testSupports()
   {
      assertThat(adapter.supports(match("hello"))).isTrue();
      assertThat(adapter.supports(new Object())).isFalse();
   }

   @Test
   @DisplayName("writes a successful invocation as a JSON ApiResponse")
   public void testWritesApiResponse() throws Exception
   {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hello");
      request.addParameter("name", "james");
      MockHttpServletResponse response = new MockHttpServletResponse();

      assertThat(adapter.handle(request, response, match("hello"))).isNull();

      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getContentType()).isEqualTo("application/json");
      assertThat(response.getContentAsString()).contains("\"data\":\"hello james\"");
   }

   @Test
   @DisplayName("writes a binding failure as a 400 problem detail")
   public void testWritesBadRequestProblem() throws Exception
   {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hello");
      MockHttpServletResponse response = new MockHttpServletResponse();

      adapter.handle(request, response, match("hello"));

      assertThat(response.getStatus()).isEqualTo(400);
      assertThat(response.getContentType()).isEqualTo("application/problem+json");
      assertThat(response.getContentAsString())
         .contains("\"title\":\"Bad Request\"")
         .contains("\"status\":400")
         .contains("\"instance\":\"/hello\"")
         .contains("\"code\":\"BadRequestException\"");
   }

   @Test
   @DisplayName("writes a handling failure as a 500 problem detail")
   public void testWritesInternalServerErrorProblem() throws Exception
   {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/boom");
      MockHttpServletResponse response = new MockHttpServletResponse();

      adapter.handle(request, response, match("boom"));

      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getContentType()).isEqualTo("application/problem+json");
      assertThat(response.getContentAsString())
         .contains("\"title\":\"Internal Server Error\"")
         .contains("\"status\":500");
   }

   private static RouteMatch match(final String methodName)
   {
      for (Method method : TestAdapter.class.getMethods())
      {
         if (method.getName().equals(methodName))
         {
            return new RouteMatch(new HandlerMethod(new TestAdapter(), method), Map.of());
         }
      }

      throw new AssertionError("No such method: " + methodName);
   }
}
