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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ApiRouteMatcher}.
 *
 * @author James Amoore
 */
@DisplayName("ApiRouteMatcher")
public class ApiRouteMatcherTest
{
   private final ApiRouteMatcher matcher = new ApiRouteMatcher(FakePathTemplates.PARSER);

   @Test
   @DisplayName("matches a static route exactly")
   public void testMatchesStaticRoute()
   {
      HandlerMethod handler = handler();
      matcher.registerRoute(new ApiRoute("/users", HttpVerb.GET), handler);

      RouteMatch match = matcher.lookupPath(new FakeRestRequest("GET", "/users"));

      assertThat(match.handler()).isSameAs(handler);
      assertThat(match.pathVariables()).isEmpty();
   }

   @Test
   @DisplayName("matches a templated route and captures its variables")
   public void testMatchesTemplatedRoute()
   {
      HandlerMethod handler = handler();
      matcher.registerRoute(new ApiRoute("/users/{id}", HttpVerb.GET), handler);

      RouteMatch match = matcher.lookupPath(new FakeRestRequest("GET", "/users/42"));

      assertThat(match.handler()).isSameAs(handler);
      assertThat(match.pathVariables()).containsEntry("id", "42");
   }

   @Test
   @DisplayName("prefers the more specific of two templated routes")
   public void testPrefersMoreSpecificTemplate()
   {
      HandlerMethod specific = handler();
      HandlerMethod general = handler();
      matcher.registerRoute(new ApiRoute("/users/{id}/{section}", HttpVerb.GET), general);
      matcher.registerRoute(new ApiRoute("/users/{id}/games", HttpVerb.GET), specific);

      RouteMatch match = matcher.lookupPath(new FakeRestRequest("GET", "/users/42/games"));

      assertThat(match.handler()).isSameAs(specific);
   }

   @Test
   @DisplayName("skips templated routes registered for another verb")
   public void testSkipsOtherVerbs()
   {
      matcher.registerRoute(new ApiRoute("/users/{id}", HttpVerb.GET), handler());

      assertThat(matcher.lookupPath(new FakeRestRequest("POST", "/users/42"))).isNull();
   }

   @Test
   @DisplayName("returns null when no route matches")
   public void testNoMatch()
   {
      matcher.registerRoute(new ApiRoute("/users", HttpVerb.GET), handler());
      matcher.registerRoute(new ApiRoute("/users/{id}", HttpVerb.GET), handler());

      assertThat(matcher.lookupPath(new FakeRestRequest("GET", "/games/42"))).isNull();
   }

   @Test
   @DisplayName("returns null for an unknown verb")
   public void testUnknownVerb()
   {
      matcher.registerRoute(new ApiRoute("/users", HttpVerb.GET), handler());

      assertThat(matcher.lookupPath(new FakeRestRequest("OPTIONS", "/users"))).isNull();
   }

   @Test
   @DisplayName("treats an empty path as the root path")
   public void testEmptyPath()
   {
      HandlerMethod handler = handler();
      matcher.registerRoute(new ApiRoute("/", HttpVerb.GET), handler);

      assertThat(matcher.lookupPath(new FakeRestRequest("GET", "")).handler()).isSameAs(handler);
      assertThat(matcher.lookupPath(new FakeRestRequest("GET", null)).handler()).isSameAs(handler);
   }

   @Test
   @DisplayName("rejects duplicate routes")
   public void testRejectsDuplicateRoutes()
   {
      matcher.registerRoute(new ApiRoute("/users/{id}", HttpVerb.GET), handler());

      assertThatIllegalStateException()
         .isThrownBy(() -> matcher.registerRoute(new ApiRoute("/users/{name}", HttpVerb.GET), handler()))
         .withMessageContaining("Duplicate route");
   }

   @Test
   @DisplayName("allows the same path on different verbs")
   public void testSamePathDifferentVerbs()
   {
      matcher.registerRoute(new ApiRoute("/users", HttpVerb.GET), handler());
      matcher.registerRoute(new ApiRoute("/users", HttpVerb.POST), handler());

      assertThat(matcher.lookupPath(new FakeRestRequest("POST", "/users"))).isNotNull();
   }

   private static HandlerMethod handler()
   {
      Method method;
      try
      {
         method = Object.class.getMethod("toString");
      }
      catch (final NoSuchMethodException e)
      {
         throw new AssertionError(e);
      }

      return new HandlerMethod(new Object(), method);
   }
}
