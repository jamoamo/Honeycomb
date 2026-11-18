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

import io.github.jamoamo.honeycomb.adapter.argument.AdapterArgumentResolver;
import io.github.jamoamo.honeycomb.adapter.argument.DefaultValueConverter;
import io.github.jamoamo.honeycomb.adapter.argument.ValueConverter;
import io.github.jamoamo.honeycomb.rest.argument.ArgumentResolutionContext;
import io.github.jamoamo.honeycomb.rest.argument.PathVariableArgumentResolver;
import io.github.jamoamo.honeycomb.rest.argument.QueryParamArgumentResolver;
import io.github.jamoamo.honeycomb.rest.argument.RequestBodyArgumentResolver;
import io.github.jamoamo.honeycomb.rest.request.RestRequest;
import io.github.jamoamo.honeycomb.rest.response.ApiResponse;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;
import io.github.jamoamo.honeycomb.usecase.UseCaseResult;

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
 * for each parameter, the handler method is invoked, and its result or exception is converted to a
 * {@link RestResponse}; see {@link #invokeRestControllerAdapterMethod(RouteMatch, RestRequest)} for the rules.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class RestControllerAdapterMethodInvoker
{
   private final List<AdapterArgumentResolver<ArgumentResolutionContext>> argumentResolvers;
   private final ExceptionProblemDetailMapper exceptionMapper;
   private final UseCaseResultMapper useCaseResultMapper;

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
   public RestControllerAdapterMethodInvoker(
      final List<AdapterArgumentResolver<ArgumentResolutionContext>> argumentResolvers)
   {
      this(argumentResolvers, new ExceptionProblemDetailMapper());
   }

   /**
    * Constructor.
    *
    * @param argumentResolvers the ordered resolvers used to bind handler-method parameters
    * @param exceptionMapper   the mapper used to turn exceptions into problem details
    */
   RestControllerAdapterMethodInvoker(
      final List<AdapterArgumentResolver<ArgumentResolutionContext>> argumentResolvers,
      final ExceptionProblemDetailMapper exceptionMapper)
   {
      this.argumentResolvers = List.copyOf(argumentResolvers);
      this.exceptionMapper = exceptionMapper;
      this.useCaseResultMapper = new UseCaseResultMapper(exceptionMapper);
   }

   /**
    * Invokes the handler method matched for the request.
    *
    * <p>
    * The handler result is converted as follows:
    * </p>
    * <ul>
    * <li>an {@link ApiResponse} or {@link ProblemDetail} is returned as-is;</li>
    * <li>a {@link UseCaseResult} is mapped: a success to an {@link ApiResponse} carrying its output, a failure to
    * a {@link ProblemDetail} whose status is derived from the failure's error category;</li>
    * <li>anything else, including {@code null} or a {@code void} method, is wrapped in an {@link ApiResponse}.</li>
    * </ul>
    * <p>
    * Exceptions raised while binding arguments or invoking the handler are mapped to a {@link ProblemDetail}: a
    * {@link ProblemDetailException} to the problem it carries, a {@link BadRequestException} to a {@code 400}, and
    * anything else to a generic {@code 500} carrying an error id that is also logged. JVM {@link Error}s are not
    * mapped and propagate.
    * </p>
    *
    * @param match   the matched route to invoke
    * @param request the request being handled
    * @return the response that should be returned
    * @throws MissingArgumentResolverException if no argument resolver supports a handler-method parameter
    */
   @SuppressWarnings("checkstyle:IllegalCatch")
   public RestResponse invokeRestControllerAdapterMethod(final RouteMatch match, final RestRequest request)
   {
      Method handlerMethod = match.handler().method();
      ArgumentResolutionContext context = new ArgumentResolutionContext(request, match.pathVariables());

      try
      {
         Object[] args = resolveArguments(handlerMethod, context);
         Object result = handlerMethod.invoke(match.handler().bean(), args);

         return toResponse(result, request);
      }
      catch (final InvocationTargetException ex)
      {
         Throwable cause = ex.getCause() == null ? ex : ex.getCause();
         if (cause instanceof Error error)
         {
            throw error;
         }
         return exceptionMapper.map(cause, request);
      }
      catch (final MissingArgumentResolverException ex)
      {
         // A configuration error, not a request failure: fail fast rather than answering with a 500.
         throw ex;
      }
      catch (final RuntimeException | IOException | IllegalAccessException ex)
      {
         return exceptionMapper.map(ex, request);
      }
   }

   private RestResponse toResponse(final Object result, final RestRequest request)
   {
      return switch (result)
      {
         case RestResponse response -> response;
         case UseCaseResult<?> useCaseResult -> useCaseResultMapper.map(useCaseResult, request);
         case null, default -> ApiResponse.of(result);
      };
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
      for (AdapterArgumentResolver<ArgumentResolutionContext> resolver : argumentResolvers)
      {
         if (resolver.supports(parameter))
         {
            return resolver.resolve(parameter, context);
         }
      }

      throw new MissingArgumentResolverException("No argument resolver supports parameter " + parameter + ".");
   }

   private static List<AdapterArgumentResolver<ArgumentResolutionContext>> defaultResolvers(
      final ObjectMapper objectMapper)
   {
      ValueConverter valueConverter = new DefaultValueConverter();
      return List.of(
         new PathVariableArgumentResolver(valueConverter),
         new QueryParamArgumentResolver(valueConverter),
         new RequestBodyArgumentResolver(objectMapper));
   }
}
