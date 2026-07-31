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

import io.github.jamoamo.honeycomb.rest.argument.AdapterArgumentResolver;
import io.github.jamoamo.honeycomb.rest.argument.ArgumentResolutionContext;
import io.github.jamoamo.honeycomb.rest.argument.DefaultValueConverter;
import io.github.jamoamo.honeycomb.rest.argument.PathVariableArgumentResolver;
import io.github.jamoamo.honeycomb.rest.argument.QueryParamArgumentResolver;
import io.github.jamoamo.honeycomb.rest.argument.RequestBodyArgumentResolver;
import io.github.jamoamo.honeycomb.rest.argument.ValueConverter;
import io.github.jamoamo.honeycomb.rest.request.RestRequest;
import io.github.jamoamo.honeycomb.rest.response.ApiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * Invoker for methods on a rest controller adapter.
 *
 * <p>
 * Arguments are bound from the request by consulting the configured {@link AdapterArgumentResolver}s in order
 * for each parameter, the handler method is invoked, and its return value is wrapped in an
 * {@link ApiResponse} so that every endpoint produces a consistent response shape.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class RestControllerAdapterMethodInvoker
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RestControllerAdapterMethodInvoker.class);

   private final List<AdapterArgumentResolver> argumentResolvers;

   /**
    * Constructor using the default argument resolvers.
    *
    * @param objectMapper the object mapper used to deserialize request bodies
    */
   public RestControllerAdapterMethodInvoker(final ObjectMapper objectMapper)
   {
      this(defaultResolvers(objectMapper));
   }

   /**
    * Constructor.
    *
    * @param argumentResolvers the ordered resolvers used to bind handler-method parameters; the first
    *        resolver that supports a parameter resolves it, so a catch-all body resolver should be last
    */
   public RestControllerAdapterMethodInvoker(final List<AdapterArgumentResolver> argumentResolvers)
   {
      this.argumentResolvers = List.copyOf(argumentResolvers);
   }

   /**
    * Invokes the handler method matched for the request.
    *
    * @param match   the matched route to invoke
    * @param request the request being handled
    * @return the api response that should be returned
    * @throws BadRequestException if a parameter cannot be bound from the request
    * @throws RestException       if the handler method throws or cannot be executed
    */
   public ApiResponse<?> invokeRestControllerAdapterMethod(final RouteMatch match, final RestRequest request)
   {
      Method handlerMethod = match.handler().method();
      ArgumentResolutionContext context = new ArgumentResolutionContext(request, match.pathVariables());

      try
      {
         Object[] args = resolveArguments(handlerMethod, context);
         Object result = handlerMethod.invoke(match.handler().bean(), args);

         return ApiResponse.of(result);
      }
      catch (final InvocationTargetException ex)
      {
         LOGGER.error("Exception invoking handling method.", ex);
         throw new RestException("Error invoking handling method", ex.getCause());
      }
      catch (final IOException | IllegalAccessException ex)
      {
         LOGGER.error("Rest controller adapter method error.", ex);
         throw new RestException("Method couldn't be executed", ex);
      }
   }

   private Object[] resolveArguments(final Method handlerMethod, final ArgumentResolutionContext context)
      throws IOException
   {
      Parameter[] parameters = handlerMethod.getParameters();
      Object[] args = new Object[parameters.length];

      for (int i = 0; i < parameters.length; i++)
      {
         args[i] = resolveArgument(parameters[i], context);
      }

      return args;
   }

   private Object resolveArgument(final Parameter parameter, final ArgumentResolutionContext context)
      throws IOException
   {
      for (AdapterArgumentResolver resolver : argumentResolvers)
      {
         if (resolver.supports(parameter))
         {
            return resolver.resolve(parameter, context);
         }
      }

      throw new IllegalStateException("No argument resolver supports parameter " + parameter + ".");
   }

   private static List<AdapterArgumentResolver> defaultResolvers(final ObjectMapper objectMapper)
   {
      ValueConverter valueConverter = new DefaultValueConverter();
      return List.of(
         new PathVariableArgumentResolver(valueConverter),
         new QueryParamArgumentResolver(valueConverter),
         new RequestBodyArgumentResolver(objectMapper));
   }
}
