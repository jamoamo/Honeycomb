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
import io.github.jamoamo.honeycomb.rest.argument.HeaderParamArgumentResolver;
import io.github.jamoamo.honeycomb.rest.argument.PathVariableArgumentResolver;
import io.github.jamoamo.honeycomb.rest.argument.QueryParamArgumentResolver;
import io.github.jamoamo.honeycomb.rest.argument.RequestBodyArgumentResolver;
import io.github.jamoamo.honeycomb.rest.auth.CurrentPrincipalArgumentResolver;
import io.github.jamoamo.honeycomb.rest.intercept.Attributes;
import io.github.jamoamo.honeycomb.rest.intercept.EndpointInterceptor;
import io.github.jamoamo.honeycomb.rest.intercept.EndpointInvocation;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
   private static final int NO_CONTENT = 204;

   private final List<AdapterArgumentResolver<ArgumentResolutionContext>> argumentResolvers;
   private final ExceptionProblemDetailMapper exceptionMapper;
   private final FailureProblemMapper failureMapper;
   private final List<EndpointInterceptor> interceptors;

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
    * Constructor using the default argument resolvers.
    *
    * @param objectMapper  the object mapper used to deserialize request bodies
    * @param failureMapper the mapper used to turn {@link EndpointResult.Failed} results into problem details
    * @since 1.1.0
    */
   public RestControllerAdapterMethodInvoker(
      final ObjectMapper objectMapper,
      final FailureProblemMapper failureMapper)
   {
      this(defaultResolvers(objectMapper), failureMapper);
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
      this(argumentResolvers, new DefaultFailureProblemMapper());
   }

   /**
    * Constructor.
    *
    * @param argumentResolvers the ordered resolvers used to bind handler-method parameters; the first
    *        resolver that supports a parameter resolves it, so a catch-all body resolver should be last
    * @param failureMapper     the mapper used to turn {@link EndpointResult.Failed} results into problem details
    * @since 1.1.0
    */
   public RestControllerAdapterMethodInvoker(
      final List<AdapterArgumentResolver<ArgumentResolutionContext>> argumentResolvers,
      final FailureProblemMapper failureMapper)
   {
      this(argumentResolvers, failureMapper, List.of());
   }

   /**
    * Constructor using the default argument resolvers.
    *
    * @param objectMapper  the object mapper used to deserialize request bodies
    * @param failureMapper the mapper used to turn {@link EndpointResult.Failed} results into problem details
    * @param interceptors  the interceptors run around every endpoint invocation, outermost first
    * @since 1.1.0
    */
   public RestControllerAdapterMethodInvoker(
      final ObjectMapper objectMapper,
      final FailureProblemMapper failureMapper,
      final List<EndpointInterceptor> interceptors)
   {
      this(defaultResolvers(objectMapper), failureMapper, interceptors);
   }

   /**
    * Constructor.
    *
    * @param argumentResolvers the ordered resolvers used to bind handler-method parameters; the first
    *        resolver that supports a parameter resolves it, so a catch-all body resolver should be last
    * @param failureMapper     the mapper used to turn {@link EndpointResult.Failed} results into problem details
    * @param interceptors      the interceptors run around every endpoint invocation, outermost first
    * @since 1.1.0
    */
   public RestControllerAdapterMethodInvoker(
      final List<AdapterArgumentResolver<ArgumentResolutionContext>> argumentResolvers,
      final FailureProblemMapper failureMapper,
      final List<EndpointInterceptor> interceptors)
   {
      this(argumentResolvers, new ExceptionProblemDetailMapper(), failureMapper, interceptors);
   }

   /**
    * Constructor.
    *
    * @param argumentResolvers the ordered resolvers used to bind handler-method parameters
    * @param exceptionMapper   the mapper used to turn exceptions into problem details
    * @param failureMapper     the mapper used to turn failed endpoint results into problem details
    * @param interceptors      the interceptors run around every endpoint invocation, outermost first
    */
   RestControllerAdapterMethodInvoker(
      final List<AdapterArgumentResolver<ArgumentResolutionContext>> argumentResolvers,
      final ExceptionProblemDetailMapper exceptionMapper,
      final FailureProblemMapper failureMapper,
      final List<EndpointInterceptor> interceptors)
   {
      this.argumentResolvers = List.copyOf(argumentResolvers);
      this.exceptionMapper = exceptionMapper;
      this.failureMapper = Objects.requireNonNull(failureMapper, "failureMapper cannot be null");
      this.interceptors = List.copyOf(Objects.requireNonNull(interceptors, "interceptors cannot be null"));
   }

   /**
    * Invokes the handler method matched for the request.
    *
    * <p>
    * The handler result is converted as follows:
    * </p>
    * <ul>
    * <li>an {@link EndpointResult} is written with its status and headers: an {@link EndpointResult.Ok} as an
    * {@link ApiResponse} carrying its body (no body for {@code 204}), an {@link EndpointResult.Problem} as its
    * problem, and an {@link EndpointResult.Failed} as the problem the configured {@link FailureProblemMapper}
    * maps it to;</li>
    * <li>a {@link RestResponse} is returned as-is, and an {@link ApiResponse} or {@link ProblemDetail} is written
    * as the body of a response with the matching status;</li>
    * <li>a {@link UseCaseResult} is not mapped: endpoints must convert it to an {@link EndpointResult}, so a
    * handler that returns one at runtime is answered with a generic {@code 500};</li>
    * <li>anything else, including {@code null} or a {@code void} method, is wrapped in an {@link ApiResponse}.</li>
    * </ul>
    * <p>
    * Exceptions raised while binding arguments or invoking the handler are mapped to a {@link ProblemDetail}: a
    * {@link ProblemDetailException} to the problem it carries, a {@link BadRequestException} to a {@code 400}, and
    * anything else to a generic {@code 500} carrying an error id that is also logged. JVM {@link Error}s are not
    * mapped and propagate.
    * </p>
    * <p>
    * The configured {@link EndpointInterceptor}s run around the handler invocation, outermost first, sharing a
    * fresh set of per-request {@link Attributes} that argument resolvers can also read. Runtime exceptions thrown
    * by an interceptor are mapped to a {@link ProblemDetail} by the same rules, and an interceptor that returns
    * {@code null} is answered with a generic {@code 500}.
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
      try
      {
         return proceed(0, new EndpointInvocation(request, match, new Attributes()));
      }
      catch (final MissingArgumentResolverException ex)
      {
         throw ex;
      }
      catch (final RuntimeException ex)
      {
         return RestResponse.problem(exceptionMapper.map(ex, request));
      }
   }

   private RestResponse proceed(final int index, final EndpointInvocation invocation)
   {
      if (index == interceptors.size())
      {
         return invokeHandler(invocation);
      }
      EndpointInterceptor interceptor = interceptors.get(index);
      return Objects.requireNonNull(
         interceptor.intercept(invocation, next -> proceed(index + 1, next)),
         () -> "Endpoint interceptor " + interceptor + " returned null");
   }

   @SuppressWarnings("checkstyle:IllegalCatch")
   private RestResponse invokeHandler(final EndpointInvocation invocation)
   {
      RestRequest request = invocation.request();
      Method handlerMethod = invocation.method();

      try
      {
         Object[] args = resolveArguments(invocation);
         Object result = handlerMethod.invoke(invocation.match().handler().bean(), args);

         return toResponse(result, request, handlerMethod);
      }
      catch (final InvocationTargetException ex)
      {
         Throwable cause = ex.getCause() == null ? ex : ex.getCause();
         if (cause instanceof Error error)
         {
            throw error;
         }
         return RestResponse.problem(exceptionMapper.map(cause, request));
      }
      catch (final MissingArgumentResolverException ex)
      {
         // A configuration error, not a request failure: fail fast rather than answering with a 500.
         throw ex;
      }
      catch (final RuntimeException | IOException | IllegalAccessException ex)
      {
         return RestResponse.problem(exceptionMapper.map(ex, request));
      }
   }

   private RestResponse toResponse(final Object result, final RestRequest request, final Method handlerMethod)
   {
      return switch (result)
      {
         case EndpointResult<?> endpointResult -> toResponse(endpointResult, request);
         case RestResponse response -> response;
         case ApiResponse<?> body -> RestResponse.ok(body);
         case ProblemDetail problem -> RestResponse.problem(problem);
         case UseCaseResult<?> useCaseResult -> RestResponse.problem(exceptionMapper.internalServerError(
            "Handler method " + handlerMethod + " returned a UseCaseResult handling request " + request.method()
               + " " + request.path() + "; endpoints must return an EndpointResult instead",
            null,
            request));
         case null, default -> RestResponse.ok(ApiResponse.of(result));
      };
   }

   private RestResponse toResponse(final EndpointResult<?> result, final RestRequest request)
   {
      RestResponse response = switch (result)
      {
         case EndpointResult.Ok<?> ok when ok.status() == NO_CONTENT ->
            new RestResponse(ok.status(), Map.of(), Optional.empty());
         case EndpointResult.Ok<?> ok -> new RestResponse(ok.status(), Map.of(), Optional.of(body(ok.body())));
         case EndpointResult.Problem<?> problem -> RestResponse.problem(problem.problem());
         case EndpointResult.Failed<?> failed -> RestResponse.problem(Objects.requireNonNull(
            failureMapper.map(failed.errorCode(), failed.errors(), request),
            "failureMapper cannot return null"));
      };
      return response.withHeaders(result.headers());
   }

   private static ApiResponse<?> body(final Object body)
   {
      return body instanceof ApiResponse<?> response ? response : ApiResponse.of(body);
   }

   private Object[] resolveArguments(final EndpointInvocation invocation) throws IOException
   {
      Method handlerMethod = invocation.method();
      ArgumentResolutionContext context = new ArgumentResolutionContext(
         invocation.request(), invocation.match().pathVariables(), invocation.attributes());
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
         new HeaderParamArgumentResolver(valueConverter),
         new CurrentPrincipalArgumentResolver(),
         new RequestBodyArgumentResolver(objectMapper));
   }
}
