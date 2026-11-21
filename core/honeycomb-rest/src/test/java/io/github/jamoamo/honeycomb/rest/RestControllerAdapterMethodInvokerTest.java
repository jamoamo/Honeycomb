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
import io.github.jamoamo.honeycomb.rest.adapter.HeaderParam;
import io.github.jamoamo.honeycomb.rest.adapter.PathVariable;
import io.github.jamoamo.honeycomb.rest.adapter.QueryParam;
import io.github.jamoamo.honeycomb.rest.argument.ArgumentResolutionContext;
import io.github.jamoamo.honeycomb.rest.request.RestRequest;
import io.github.jamoamo.honeycomb.rest.response.ApiResponse;
import io.github.jamoamo.honeycomb.rest.response.FieldError;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;
import io.github.jamoamo.honeycomb.types.ErrorCategory;
import io.github.jamoamo.honeycomb.types.ErrorCode;
import io.github.jamoamo.honeycomb.usecase.UseCaseResult;
import io.github.jamoamo.honeycomb.usecase.validation.ValidationError;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link RestControllerAdapterMethodInvoker}.
 *
 * @author James Amoore
 */
@DisplayName("RestControllerAdapterMethodInvoker")
public class RestControllerAdapterMethodInvokerTest
{
   private static final ApiResponse<String> CUSTOM_RESPONSE =
      new ApiResponse<>(Optional.of("custom"), Optional.empty());
   private static final ProblemDetail NOT_FOUND =
      ProblemDetail.forStatus(404, "Not Found", "No such thing.", "/things/1", "THING_NOT_FOUND");
   private static final ProblemDetail CONFLICT =
      ProblemDetail.forStatus(409, "Conflict", "Already exists.", "/things", "THING_EXISTS");

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

      public String keyed(@HeaderParam("X-Idempotency-Key") final Optional<String> key)
      {
         return key.orElse("none");
      }

      public Body echo(final Body body)
      {
         return body;
      }

      public String nothing()
      {
         return null;
      }

      public void voidMethod()
      {
      }

      public ApiResponse<String> apiResponse()
      {
         return CUSTOM_RESPONSE;
      }

      public ProblemDetail problem()
      {
         return NOT_FOUND;
      }

      public String boom()
      {
         throw new IllegalStateException("kaboom - internal secret");
      }

      public String badRequest()
      {
         throw new BadRequestException("Invalid widget.");
      }

      public String problemException()
      {
         throw new ProblemDetailException(CONFLICT);
      }

      public String fatal()
      {
         throw new StackOverflowError("fatal");
      }

      public EndpointResult<String> useCaseSuccess()
      {
         return EndpointResult.from(UseCaseResult.success("done"), String::toUpperCase);
      }

      public EndpointResult<String> useCaseValidationFailure()
      {
         return EndpointResult.from(
            UseCaseResult.<String>failure(
               new ErrorCode("VALIDATION_FAILED", ErrorCategory.VALIDATION, "Rejected the input."),
               List.of(new ValidationError("name", "must not be blank"))),
            out -> out);
      }

      public EndpointResult<String> useCaseInternalFailure()
      {
         return EndpointResult.from(
            UseCaseResult.<String>failure(
               new ErrorCode("UNHANDLED_ERROR", ErrorCategory.INTERNAL, "Failed: internal secret")),
            out -> out);
      }

      public EndpointResult<String> created()
      {
         return EndpointResult.respond(
            UseCaseResult.success("thing"),
            out -> EndpointResult.created(out, URI.create("/things/1")));
      }

      public EndpointResult<String> noContent()
      {
         return EndpointResult.<String>noContent().withHeader("X-Trace", "abc");
      }

      public EndpointResult<String> endpointProblem()
      {
         return EndpointResult.<String>problem(CONFLICT).withHeader("Retry-After", "5");
      }

      public EndpointResult<String> rateLimited()
      {
         return EndpointResult.<String>failed(new ErrorCode("SLOW_DOWN", ErrorCategory.RATE_LIMITED, "Slow down."))
            .withHeader("Retry-After", "30");
      }

      public EndpointResult<ApiResponse<String>> okWithApiResponse()
      {
         return EndpointResult.ok(CUSTOM_RESPONSE);
      }

      public Object useCaseResultAsObject()
      {
         return UseCaseResult.success("leaked");
      }
   }

   public static final class UseCaseResultAdapter
   {
      public UseCaseResult<String> leaks()
      {
         return UseCaseResult.success("leaked");
      }
   }

   @Test
   @DisplayName("binds path variables and query parameters and wraps the result")
   public void testBindsAnnotatedParameters()
   {
      RouteMatch match = match("greet", Map.of("id", "42"));
      RestRequest request = new FakeRestRequest(
         "GET", "/greet/42", Map.of("greeting", List.of("hello")), "");

      ApiResponse<?> response = apiResponse(invoker.invokeRestControllerAdapterMethod(match, request));

      assertThat(response.data().orElseThrow()).isEqualTo("hello 42");
      assertThat(response.meta()).isEmpty();
   }

   @Test
   @DisplayName("binds request headers")
   public void testBindsHeaders()
   {
      RestRequest request = new FakeRestRequest(
         "GET", "/keyed", Map.of(), Map.of("x-idempotency-key", List.of("k1")), "");

      assertThat(apiResponse(invoker.invokeRestControllerAdapterMethod(match("keyed", Map.of()), request))
         .data().orElseThrow()).isEqualTo("k1");
      assertThat(apiResponse(invoker.invokeRestControllerAdapterMethod(
         match("keyed", Map.of()), new FakeRestRequest("GET", "/keyed"))).data().orElseThrow()).isEqualTo("none");
   }

   @Test
   @DisplayName("binds the request body through the fallback resolver")
   public void testBindsRequestBody()
   {
      RouteMatch match = match("echo", Map.of());
      RestRequest request = new FakeRestRequest("POST", "/echo", Map.of(), "{\"name\":\"james\"}");

      ApiResponse<?> response = apiResponse(invoker.invokeRestControllerAdapterMethod(match, request));

      assertThat(response.data().orElseThrow()).isEqualTo(new Body("james"));
   }

   @Test
   @DisplayName("wraps a null result as an empty response")
   public void testWrapsNullResult()
   {
      assertThat(apiResponse(invoke("nothing")).data()).isEmpty();
   }

   @Test
   @DisplayName("wraps a void method as an empty response")
   public void testWrapsVoidResult()
   {
      assertThat(apiResponse(invoke("voidMethod")).data()).isEmpty();
   }

   @Test
   @DisplayName("writes an ApiResponse result as-is with a 200 status")
   public void testReturnsApiResponseAsIs()
   {
      RestResponse response = invoke("apiResponse");

      assertThat(response.status()).isEqualTo(200);
      assertThat(response.body()).containsSame(CUSTOM_RESPONSE);
   }

   @Test
   @DisplayName("writes a ProblemDetail result as-is with its status")
   public void testReturnsProblemDetailAsIs()
   {
      RestResponse response = invoke("problem");

      assertThat(response.status()).isEqualTo(404);
      assertThat(response.body()).containsSame(NOT_FOUND);
   }

   @Test
   @DisplayName("maps an unexpected handler exception to a generic 500 problem detail")
   public void testHandlerExceptionMapsToInternalServerError()
   {
      ProblemDetail problem = problemDetail(invoke("boom"));

      assertThat(problem)
         .usingRecursiveComparison()
         .ignoringFields("errorId")
         .isEqualTo(ProblemDetail.internalServerError(
            ExceptionProblemDetailMapper.INTERNAL_SERVER_ERROR_DETAIL, "/boom", null));
      assertThat(problem.errorId()).isNotBlank();
   }

   @Test
   @DisplayName("rethrows a JVM Error thrown by the handler instead of mapping it")
   public void testHandlerErrorIsRethrown()
   {
      assertThatExceptionOfType(StackOverflowError.class).isThrownBy(() -> invoke("fatal"));
   }

   @Test
   @DisplayName("maps an unexpected runtime exception from an argument resolver to a 500 problem detail")
   public void testResolverRuntimeExceptionMapsTo500()
   {
      RestControllerAdapterMethodInvoker throwing = new RestControllerAdapterMethodInvoker(
         List.of(resolverThrowing(new IllegalArgumentException("resolver bug"))));

      ProblemDetail problem = problemDetail(throwing.invokeRestControllerAdapterMethod(
         match("echo", Map.of()), new FakeRestRequest("POST", "/echo")));

      assertThat(problem.status()).isEqualTo(500);
      assertThat(problem.detail()).isEqualTo(ExceptionProblemDetailMapper.INTERNAL_SERVER_ERROR_DETAIL);
      assertThat(problem.errorId()).isNotBlank();
   }

   @Test
   @DisplayName("returns the problem carried by a ProblemDetailException thrown from an argument resolver")
   public void testResolverProblemDetailExceptionReturnsCarriedProblem()
   {
      RestControllerAdapterMethodInvoker throwing = new RestControllerAdapterMethodInvoker(
         List.of(resolverThrowing(new ProblemDetailException(CONFLICT))));

      assertThat(throwing.invokeRestControllerAdapterMethod(
         match("echo", Map.of()), new FakeRestRequest("POST", "/echo")).body()).containsSame(CONFLICT);
   }

   @Test
   @DisplayName("writes a successful endpoint result as a 200 ApiResponse carrying the mapped output")
   public void testUseCaseSuccessMapsToApiResponse()
   {
      RestResponse response = invoke("useCaseSuccess");

      assertThat(response.status()).isEqualTo(200);
      assertThat(apiResponse(response).data().orElseThrow()).isEqualTo("DONE");
   }

   @Test
   @DisplayName("writes a created endpoint result as a 201 with a Location header")
   public void testCreatedEndpointResult()
   {
      RestResponse response = invoke("created");

      assertThat(response.status()).isEqualTo(201);
      assertThat(response.headers()).containsEntry("Location", List.of("/things/1"));
      assertThat(apiResponse(response).data().orElseThrow()).isEqualTo("thing");
   }

   @Test
   @DisplayName("writes a no content endpoint result as a 204 without a body, keeping its headers")
   public void testNoContentEndpointResult()
   {
      RestResponse response = invoke("noContent");

      assertThat(response.status()).isEqualTo(204);
      assertThat(response.body()).isEmpty();
      assertThat(response.headers()).containsEntry("X-Trace", List.of("abc"));
   }

   @Test
   @DisplayName("writes a problem endpoint result as its problem, keeping its headers")
   public void testProblemEndpointResult()
   {
      RestResponse response = invoke("endpointProblem");

      assertThat(response.status()).isEqualTo(409);
      assertThat(response.body()).containsSame(CONFLICT);
      assertThat(response.headers()).containsEntry("Retry-After", List.of("5"));
   }

   @Test
   @DisplayName("writes a failed endpoint result through the failure mapper, keeping its headers")
   public void testFailedEndpointResultKeepsHeaders()
   {
      RestResponse response = invoke("rateLimited");

      assertThat(response.status()).isEqualTo(429);
      assertThat(problemDetail(response).code()).isEqualTo("SLOW_DOWN");
      assertThat(response.headers()).containsEntry("Retry-After", List.of("30"));
   }

   @Test
   @DisplayName("writes an ApiResponse body of an ok endpoint result as-is")
   public void testOkEndpointResultWithApiResponseBody()
   {
      assertThat(invoke("okWithApiResponse").body()).containsSame(CUSTOM_RESPONSE);
   }

   @Test
   @DisplayName("maps failed endpoint results with the configured failure mapper")
   public void testCustomFailureMapper()
   {
      FailureProblemMapper teapot = (code, errors, request) ->
         ProblemDetail.forStatus(418, "I'm a teapot", code.code(), request.path(), code.code());
      RestControllerAdapterMethodInvoker custom =
         new RestControllerAdapterMethodInvoker(JsonMapper.builder().build(), teapot);

      RestResponse response = custom.invokeRestControllerAdapterMethod(
         match("useCaseValidationFailure", Map.of()), new FakeRestRequest("GET", "/useCaseValidationFailure"));

      assertThat(response.status()).isEqualTo(418);
      assertThat(problemDetail(response).detail()).isEqualTo("VALIDATION_FAILED");
   }

   @Test
   @DisplayName("answers a UseCaseResult returned at runtime with a generic 500 instead of mapping it")
   public void testUseCaseResultReturnedAtRuntimeMapsTo500()
   {
      ProblemDetail problem = problemDetail(invoke("useCaseResultAsObject"));

      assertThat(problem.status()).isEqualTo(500);
      assertThat(problem.detail()).isEqualTo(ExceptionProblemDetailMapper.INTERNAL_SERVER_ERROR_DETAIL);
      assertThat(problem.errorId()).isNotBlank();
   }

   @Test
   @DisplayName("rejects a handler method declaring a UseCaseResult return type")
   public void testHandlerMethodRejectsUseCaseResultReturnType() throws NoSuchMethodException
   {
      Method leaks = UseCaseResultAdapter.class.getMethod("leaks");

      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> new HandlerMethod(new UseCaseResultAdapter(), leaks))
         .withMessageContaining("EndpointResult");
   }

   @Test
   @DisplayName("maps a validation use case failure to a 400 problem detail carrying the field errors")
   public void testUseCaseValidationFailureMapsTo400()
   {
      ProblemDetail problem = problemDetail(invoke("useCaseValidationFailure"));

      assertThat(problem).isEqualTo(
         ProblemDetail.forStatus(400, "Bad Request", "Rejected the input.", "/useCaseValidationFailure",
               "VALIDATION_FAILED")
            .withErrors(List.of(new FieldError("name", "must not be blank"))));
   }

   @Test
   @DisplayName("maps an internal use case failure to a generic 500 problem detail with an error id")
   public void testUseCaseInternalFailureMapsTo500()
   {
      ProblemDetail problem = problemDetail(invoke("useCaseInternalFailure"));

      assertThat(problem.status()).isEqualTo(500);
      assertThat(problem.code()).isEqualTo("UNHANDLED_ERROR");
      assertThat(problem.detail()).isEqualTo(ExceptionProblemDetailMapper.INTERNAL_SERVER_ERROR_DETAIL);
      assertThat(problem.errorId()).isNotBlank();
   }

   @Test
   @DisplayName("maps a BadRequestException thrown by the handler to a 400 problem detail")
   public void testHandlerBadRequestMapsTo400()
   {
      ProblemDetail problem = problemDetail(invoke("badRequest"));

      assertThat(problem.status()).isEqualTo(400);
      assertThat(problem.detail()).isEqualTo("Invalid widget.");
      assertThat(problem.instance()).isEqualTo("/badRequest");
   }

   @Test
   @DisplayName("returns the problem detail carried by a ProblemDetailException as-is")
   public void testProblemDetailExceptionReturnsCarriedProblem()
   {
      assertThat(invoke("problemException").body()).containsSame(CONFLICT);
   }

   @Test
   @DisplayName("maps an argument binding failure to a 400 problem detail")
   public void testBindingFailureMapsTo400()
   {
      RouteMatch match = match("greet", Map.of("id", "not-a-number"));
      RestRequest request = new FakeRestRequest(
         "GET", "/greet/x", Map.of("greeting", List.of("hello")), "");

      ProblemDetail problem = problemDetail(invoker.invokeRestControllerAdapterMethod(match, request));

      assertThat(problem).isEqualTo(
         ProblemDetail.badRequest("Invalid value for parameter 'id'.", "/greet/x", null));
   }

   @Test
   @DisplayName("maps a request read failure to a generic 500 problem detail")
   public void testBodyReadFailureMapsTo500()
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

      ProblemDetail problem =
         problemDetail(invoker.invokeRestControllerAdapterMethod(match("echo", Map.of()), failing));

      assertThat(problem.status()).isEqualTo(500);
      assertThat(problem.detail()).isEqualTo(ExceptionProblemDetailMapper.INTERNAL_SERVER_ERROR_DETAIL);
   }

   @Test
   @DisplayName("fails when no resolver supports a parameter")
   public void testNoResolverSupportsParameter()
   {
      RestControllerAdapterMethodInvoker noResolvers = new RestControllerAdapterMethodInvoker(List.of());

      assertThatExceptionOfType(MissingArgumentResolverException.class)
         .isThrownBy(() -> noResolvers.invokeRestControllerAdapterMethod(
            match("echo", Map.of()), new FakeRestRequest("POST", "/echo")))
         .withMessageContaining("No argument resolver");
   }

   @Test
   @DisplayName("invokes a parameterless method with custom resolvers")
   public void testParameterlessMethodWithCustomResolvers()
   {
      RestControllerAdapterMethodInvoker noResolvers = new RestControllerAdapterMethodInvoker(List.of());

      ApiResponse<?> response = apiResponse(noResolvers.invokeRestControllerAdapterMethod(
         match("nothing", Map.of()), new FakeRestRequest("GET", "/nothing")));

      assertThat(response.data()).isEmpty();
   }

   @Test
   @DisplayName("wraps a data value via the Optional-based response factory")
   public void testResponseFactory()
   {
      ApiResponse<String> response = ApiResponse.of("value");

      assertThat(response.data()).contains("value");
      assertThat(response.meta()).isEmpty();
   }

   private RestResponse invoke(final String methodName)
   {
      return invoker.invokeRestControllerAdapterMethod(
         match(methodName, Map.of()), new FakeRestRequest("GET", "/" + methodName));
   }

   private static ApiResponse<?> apiResponse(final RestResponse response)
   {
      assertThat(response.body()).containsInstanceOf(ApiResponse.class);
      return (ApiResponse<?>) response.body().orElseThrow();
   }

   private static ProblemDetail problemDetail(final RestResponse response)
   {
      assertThat(response.body()).containsInstanceOf(ProblemDetail.class);
      ProblemDetail problem = (ProblemDetail) response.body().orElseThrow();
      assertThat(response.status()).isEqualTo(problem.status());
      return problem;
   }

   private static AdapterArgumentResolver<ArgumentResolutionContext> resolverThrowing(final RuntimeException ex)
   {
      return new AdapterArgumentResolver<>()
      {
         @Override
         public boolean supports(final Parameter parameter)
         {
            return true;
         }

         @Override
         public Object resolve(final Parameter parameter, final ArgumentResolutionContext context)
         {
            throw ex;
         }
      };
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
}
