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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServletRestRequest}.
 *
 * @author James Amoore
 */
@DisplayName("ServletRestRequest")
public class ServletRestRequestTest
{
   @Test
   @DisplayName("exposes the request method")
   public void testMethod()
   {
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users");

      assertThat(new ServletRestRequest(request).method()).isEqualTo("POST");
   }

   @Test
   @DisplayName("strips the context path from the request path")
   public void testPathStripsContextPath()
   {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/users/42");
      request.setContextPath("/app");

      assertThat(new ServletRestRequest(request).path()).isEqualTo("/users/42");
   }

   @Test
   @DisplayName("exposes an empty path as the root path")
   public void testEmptyPath()
   {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app");
      request.setContextPath("/app");

      assertThat(new ServletRestRequest(request).path()).isEqualTo("/");
   }

   @Test
   @DisplayName("exposes query parameter values")
   public void testQueryParameterValues()
   {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");
      request.addParameter("tag", "a", "b");

      ServletRestRequest restRequest = new ServletRestRequest(request);

      assertThat(restRequest.queryParameterValues("tag")).containsExactly("a", "b");
      assertThat(restRequest.queryParameterValues("missing")).isEmpty();
   }

   @Test
   @DisplayName("exposes the request body")
   public void testBody() throws IOException
   {
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users");
      request.setContent("{\"name\":\"james\"}".getBytes(StandardCharsets.UTF_8));

      assertThat(new ServletRestRequest(request).body().readAllBytes())
         .asString(StandardCharsets.UTF_8).isEqualTo("{\"name\":\"james\"}");
   }
}
