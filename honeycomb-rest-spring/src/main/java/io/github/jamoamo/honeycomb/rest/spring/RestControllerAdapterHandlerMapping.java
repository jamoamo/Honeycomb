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

import io.github.jamoamo.honeycomb.rest.ApiRoute;
import io.github.jamoamo.honeycomb.rest.ApiRouteMatcher;
import io.github.jamoamo.honeycomb.rest.HandlerMethod;
import io.github.jamoamo.honeycomb.rest.PathTemplateParser;
import io.github.jamoamo.honeycomb.rest.adapter.Endpoint;
import io.github.jamoamo.honeycomb.rest.adapter.PathVariable;
import io.github.jamoamo.honeycomb.rest.adapter.QueryParam;
import io.github.jamoamo.honeycomb.rest.adapter.RestControllerAdapter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Handler mapping that registers the {@link Endpoint} methods of every bean annotated with
 * {@link RestControllerAdapter} and matches incoming requests to them.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class RestControllerAdapterHandlerMapping extends AbstractHandlerMapping
{
   private final ApiRouteMatcher matcher;

   /**
    * Constructor.
    *
    * @param pathTemplateParser the parser used to compile endpoint route templates
    */
   public RestControllerAdapterHandlerMapping(final PathTemplateParser pathTemplateParser)
   {
      this.matcher = new ApiRouteMatcher(pathTemplateParser);
   }

   /**
    * Registers the routes of every {@link RestControllerAdapter} bean in the application context.
    */
   @Override
   protected void initApplicationContext()
   {
      super.initApplicationContext();
      getApplicationContext().getBeansWithAnnotation(RestControllerAdapter.class)
         .forEach((name, bean) -> registerRoutes(bean));
   }

   /**
    * Looks up the handler for the request.
    *
    * @param request the request to find a handler for
    * @return the matched route, or {@code null} when no route matches
    */
   @Override
   protected Object getHandlerInternal(final HttpServletRequest request)
   {
      return matcher.lookupPath(new ServletRestRequest(request));
   }

   private void registerRoutes(final Object adapterBean)
   {
      RestControllerAdapter adapter = adapterBean.getClass().getAnnotation(RestControllerAdapter.class);
      String basePath = basePath(adapter);

      for (Method m : adapterBean.getClass().getMethods())
      {
         Endpoint ep = m.getAnnotation(Endpoint.class);
         if (ep != null)
         {
            validateEndpointParameters(m);
            matcher.registerRoute(
               new ApiRoute(basePath + ep.path(), ep.method()),
               new HandlerMethod(adapterBean, m));
         }
      }
   }

   private static String basePath(final RestControllerAdapter adapter)
   {
      if (!adapter.fullPath().isEmpty())
      {
         return adapter.fullPath();
      }
      if (!adapter.version().isEmpty())
      {
         return "/api/" + adapter.version() + adapter.apiPath();
      }

      return adapter.apiPath();
   }

   private static void validateEndpointParameters(final Method method)
   {
      long bodyParameters = Arrays.stream(method.getParameters())
         .filter(RestControllerAdapterHandlerMapping::isBodyParameter)
         .count();
      if (bodyParameters > 1)
      {
         throw new IllegalStateException(
            "@Endpoint methods may declare at most one request-body parameter: " + method);
      }
   }

   private static boolean isBodyParameter(final Parameter parameter)
   {
      return !parameter.isAnnotationPresent(PathVariable.class)
         && !parameter.isAnnotationPresent(QueryParam.class);
   }
}
