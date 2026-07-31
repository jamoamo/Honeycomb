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

import io.github.jamoamo.honeycomb.rest.HttpVerb;
import io.github.jamoamo.honeycomb.rest.RouteMatch;
import io.github.jamoamo.honeycomb.rest.adapter.Endpoint;
import io.github.jamoamo.honeycomb.rest.adapter.PathVariable;
import io.github.jamoamo.honeycomb.rest.adapter.QueryParam;
import io.github.jamoamo.honeycomb.rest.adapter.RestControllerAdapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link RestControllerAdapterHandlerMapping}.
 *
 * @author James Amoore
 */
@DisplayName("RestControllerAdapterHandlerMapping")
public class RestControllerAdapterHandlerMappingTest
{
   @RestControllerAdapter(version = "v1", apiPath = "/users")
   public static class VersionedAdapter
   {
      @Endpoint(path = "/{id}", method = HttpVerb.GET)
      public String find(@PathVariable("id") final int id)
      {
         return "user " + id;
      }
   }

   @RestControllerAdapter(fullPath = "/internal/users")
   public static class FullPathAdapter
   {
      @Endpoint(path = "", method = HttpVerb.GET)
      public String list()
      {
         return "users";
      }
   }

   @RestControllerAdapter(apiPath = "/plain")
   public static class PlainAdapter
   {
      @Endpoint(path = "/things", method = HttpVerb.POST)
      public String create(final String body)
      {
         return body;
      }

      public String notAnEndpoint()
      {
         return "ignored";
      }
   }

   @RestControllerAdapter(apiPath = "/broken")
   public static class TwoBodyAdapter
   {
      @Endpoint(path = "/things", method = HttpVerb.POST)
      public String create(final String first, final String second, @QueryParam("q") final String query)
      {
         return first + second + query;
      }
   }

   @Test
   @DisplayName("registers endpoints under the /api/{version}{apiPath} base path")
   public void testVersionedBasePath()
   {
      RestControllerAdapterHandlerMapping mapping = mapping(VersionedAdapter.class);

      Object handler = mapping.getHandlerInternal(new MockHttpServletRequest("GET", "/api/v1/users/42"));

      assertThat(handler).isInstanceOf(RouteMatch.class);
      assertThat(((RouteMatch) handler).pathVariables()).containsEntry("id", "42");
   }

   @Test
   @DisplayName("registers endpoints under the declared full path")
   public void testFullPathBasePath()
   {
      RestControllerAdapterHandlerMapping mapping = mapping(FullPathAdapter.class);

      assertThat(mapping.getHandlerInternal(new MockHttpServletRequest("GET", "/internal/users"))).isNotNull();
   }

   @Test
   @DisplayName("registers endpoints under the api path when no version is declared")
   public void testPlainBasePath()
   {
      RestControllerAdapterHandlerMapping mapping = mapping(PlainAdapter.class);

      assertThat(mapping.getHandlerInternal(new MockHttpServletRequest("POST", "/plain/things"))).isNotNull();
      assertThat(mapping.getHandlerInternal(new MockHttpServletRequest("GET", "/plain/things"))).isNull();
   }

   @Test
   @DisplayName("returns null for an unmatched request")
   public void testUnmatchedRequest()
   {
      RestControllerAdapterHandlerMapping mapping = mapping(VersionedAdapter.class);

      assertThat(mapping.getHandlerInternal(new MockHttpServletRequest("GET", "/nowhere"))).isNull();
   }

   @Test
   @DisplayName("rejects endpoints declaring more than one body parameter")
   public void testRejectsMultipleBodyParameters()
   {
      assertThatExceptionOfType(BeanCreationException.class)
         .isThrownBy(() -> mapping(TwoBodyAdapter.class))
         .withRootCauseInstanceOf(IllegalStateException.class);
   }

   private static RestControllerAdapterHandlerMapping mapping(final Class<?> adapterClass)
   {
      StaticWebApplicationContext context = new StaticWebApplicationContext();
      context.registerSingleton("adapter", adapterClass);
      context.registerBean(
         RestControllerAdapterHandlerMapping.class,
         () -> new RestControllerAdapterHandlerMapping(new PathPatternTemplateParser()));
      context.refresh();

      return context.getBean(RestControllerAdapterHandlerMapping.class);
   }
}
