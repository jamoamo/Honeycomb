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

import io.github.jamoamo.honeycomb.rest.request.RestRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A matcher that matches a request to a handling method.
 *
 * <p>
 * Route paths are compiled into {@link PathTemplate}s via an injected {@link PathTemplateParser}. Static routes
 * (those declaring no path variables) are matched by an exact lookup; templated routes are matched in order of
 * decreasing specificity so that a more specific template wins over a less specific one.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class ApiRouteMatcher
{
   private final PathTemplateParser templateParser;
   private final Map<ApiRoute, HandlerMethod> staticRoutes = new HashMap<>();
   private final List<CompiledRoute> templatedRoutes = new ArrayList<>();
   private final Set<String> registeredSignatures = new HashSet<>();

   /**
    * Constructor.
    *
    * @param templateParser the parser used to compile route paths into templates
    */
   public ApiRouteMatcher(final PathTemplateParser templateParser)
   {
      this.templateParser = templateParser;
   }

   /**
    * Registers a route with the matcher.
    *
    * @param route  the route to match
    * @param method the method to use for the route
    */
   public void registerRoute(final ApiRoute route, final HandlerMethod method)
   {
      String signature = route.method() + " " + normalizeTemplate(route.path());
      if (!registeredSignatures.add(signature))
      {
         throw new IllegalStateException("Duplicate route registered: " + route);
      }

      PathTemplate template = templateParser.parse(route.path());
      if (template.isStatic())
      {
         staticRoutes.put(route, method);
      }
      else
      {
         templatedRoutes.add(new CompiledRoute(route.method(), template, method));
         templatedRoutes.sort(Comparator.comparing(CompiledRoute::template));
      }
   }

   /**
    * Looks up the handling method for the request.
    *
    * @param request the request to find a handling method for
    * @return the matched route, or {@code null} if no route matches the request
    */
   public RouteMatch lookupPath(final RestRequest request)
   {
      HttpVerb verb = parseVerb(request.method());
      if (verb == null)
      {
         return null;
      }

      String path = request.path();
      if (path == null || path.isEmpty())
      {
         path = "/";
      }

      return match(path, verb);
   }

   private RouteMatch match(final String path, final HttpVerb verb)
   {
      HandlerMethod staticHandler = staticRoutes.get(new ApiRoute(path, verb));
      if (staticHandler != null)
      {
         return new RouteMatch(staticHandler, Map.of());
      }

      for (CompiledRoute route : templatedRoutes)
      {
         if (route.method() != verb)
         {
            continue;
         }

         Optional<Map<String, String>> variables = route.template().match(path);
         if (variables.isPresent())
         {
            return new RouteMatch(route.handler(), variables.get());
         }
      }

      return null;
   }

   private static HttpVerb parseVerb(final String method)
   {
      for (HttpVerb verb : HttpVerb.values())
      {
         if (verb.name().equals(method))
         {
            return verb;
         }
      }

      return null;
   }

   private static String normalizeTemplate(final String path)
   {
      return path.replaceAll("\\{[^{}/]*\\}", "{}");
   }

   private static final class CompiledRoute
   {
      private final HttpVerb method;
      private final PathTemplate template;
      private final HandlerMethod handler;

      CompiledRoute(final HttpVerb method, final PathTemplate template, final HandlerMethod handler)
      {
         this.method = method;
         this.template = template;
         this.handler = handler;
      }

      HttpVerb method()
      {
         return method;
      }

      PathTemplate template()
      {
         return template;
      }

      HandlerMethod handler()
      {
         return handler;
      }
   }
}
