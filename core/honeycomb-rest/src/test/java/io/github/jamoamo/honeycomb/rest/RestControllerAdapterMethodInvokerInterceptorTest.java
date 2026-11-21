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
import io.github.jamoamo.honeycomb.rest.argument.ArgumentResolutionContext;
import io.github.jamoamo.honeycomb.rest.intercept.AttributeKey;
import io.github.jamoamo.honeycomb.rest.intercept.EndpointInterceptor;
import io.github.jamoamo.honeycomb.rest.intercept.EndpointInvocation;
import io.github.jamoamo.honeycomb.rest.response.ApiResponse;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for the {@link EndpointInterceptor} chain run by {@link RestControllerAdapterMethodInvoker}.
 *
 * @author James Amoore
 */
@DisplayName("RestControllerAdapterMethodInvoker interceptors")
public class RestControllerAdapterMethodInvokerInterceptorTest
{
   private static final AttributeKey<String> USER = AttributeKey.of("user", String.class);
   private static final ProblemDetail UNAUTHORIZED =
      ProblemDetail.forStatus(401, "Unauthorized", "No token.", "/things", "UNAUTHORIZED");

   private final List<String> calls = new ArrayList<>();
   private final Adapter adapter = new Adapter(calls);

   public static final class Adapter
   {
      private final List<String> calls;

      Adapter(final List<String> calls)
      {
         this.calls = calls;
      }

      public String hello()
      {
         calls.add("handler");
         return "hello";
      }

      public String echo(final String value)
      {
         return value;
      }

      public String fail()
      {
         throw new IllegalStateException("boom");
      }
   }

   @Test
   @DisplayName("runs interceptors in order, each wrapping the next")
   public void testRunsInterceptorsInOrder()
   {
      RestResponse response = invoker(recording("outer"), recording("inner")).invokeRestControllerAdapterMethod(
         match("hello"), new FakeRestRequest("GET", "/hello"));

      assertThat(data(response)).isEqualTo("hello");
      assertThat(calls).containsExactly("outer:before", "inner:before", "handler", "inner:after", "outer:after");
   }

   @Test
   @DisplayName("lets an interceptor short-circuit without invoking the handler")
   public void testShortCircuit()
   {
      RestResponse response = invoker((invocation, chain) -> RestResponse.problem(UNAUTHORIZED))
         .invokeRestControllerAdapterMethod(match("hello"), new FakeRestRequest("GET", "/hello"));

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.body()).contains(UNAUTHORIZED);
      assertThat(calls).isEmpty();
   }

   @Test
   @DisplayName("answers a ProblemDetailException thrown by an interceptor with its problem")
   public void testInterceptorProblemDetailException()
   {
      RestResponse response = invoker((invocation, chain) -> {
         throw new ProblemDetailException(UNAUTHORIZED);
      }).invokeRestControllerAdapterMethod(match("hello"), new FakeRestRequest("GET", "/hello"));

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.body()).contains(UNAUTHORIZED);
      assertThat(calls).isEmpty();
   }

   @Test
   @DisplayName("answers any other interceptor exception with a generic 500")
   public void testInterceptorRuntimeException()
   {
      RestResponse response = invoker((invocation, chain) -> {
         throw new IllegalStateException("interceptor bug");
      }).invokeRestControllerAdapterMethod(match("hello"), new FakeRestRequest("GET", "/hello"));

      assertThat(response.status()).isEqualTo(500);
      assertThat(response.body()).containsInstanceOf(ProblemDetail.class);
   }

   @Test
   @DisplayName("answers an interceptor that returns null with a generic 500")
   public void testInterceptorReturnsNull()
   {
      RestResponse response = invoker((invocation, chain) -> null)
         .invokeRestControllerAdapterMethod(match("hello"), new FakeRestRequest("GET", "/hello"));

      assertThat(response.status()).isEqualTo(500);
   }

   @Test
   @DisplayName("hands handler failures to interceptors as problem responses")
   public void testHandlerFailureSeenAsResponse()
   {
      List<Integer> seen = new ArrayList<>();
      RestResponse response = invoker((invocation, chain) -> {
         RestResponse inner = chain.proceed(invocation);
         seen.add(inner.status());
         return inner;
      }).invokeRestControllerAdapterMethod(match("fail"), new FakeRestRequest("GET", "/fail"));

      assertThat(response.status()).isEqualTo(500);
      assertThat(seen).containsExactly(500);
   }

   @Test
   @DisplayName("shares attributes and a replaced request with argument resolvers")
   public void testAttributesAndRequestReachResolvers()
   {
      AdapterArgumentResolver<ArgumentResolutionContext> resolver = new AdapterArgumentResolver<>()
      {
         @Override
         public boolean supports(final Parameter parameter)
         {
            return true;
         }

         @Override
         public Object resolve(final Parameter parameter, final ArgumentResolutionContext context)
         {
            return context.attributes().get(USER).orElse("anonymous") + "@" + context.request().path();
         }
      };
      EndpointInterceptor interceptor = (invocation, chain) -> {
         invocation.attributes().put(USER, "james");
         return chain.proceed(new EndpointInvocation(
            new FakeRestRequest("GET", "/replaced"), invocation.match(), invocation.attributes()));
      };
      RestControllerAdapterMethodInvoker invoker = new RestControllerAdapterMethodInvoker(
         List.of(resolver), new DefaultFailureProblemMapper(), List.of(interceptor));

      RestResponse response = invoker.invokeRestControllerAdapterMethod(
         match("echo", String.class), new FakeRestRequest("GET", "/echo"));

      assertThat(data(response)).isEqualTo("james@/replaced");
   }

   @Test
   @DisplayName("gives each request fresh attributes")
   public void testFreshAttributesPerRequest()
   {
      List<Boolean> present = new ArrayList<>();
      RestControllerAdapterMethodInvoker invoker = invoker((invocation, chain) -> {
         present.add(invocation.attributes().get(USER).isPresent());
         invocation.attributes().put(USER, "james");
         return chain.proceed(invocation);
      });

      invoker.invokeRestControllerAdapterMethod(match("hello"), new FakeRestRequest("GET", "/hello"));
      invoker.invokeRestControllerAdapterMethod(match("hello"), new FakeRestRequest("GET", "/hello"));

      assertThat(present).containsExactly(false, false);
   }

   @Test
   @DisplayName("still fails fast on a missing argument resolver behind interceptors")
   public void testMissingArgumentResolverPropagates()
   {
      RestControllerAdapterMethodInvoker invoker = new RestControllerAdapterMethodInvoker(
         List.of(), new DefaultFailureProblemMapper(), List.of(recording("outer")));

      assertThatExceptionOfType(MissingArgumentResolverException.class)
         .isThrownBy(() -> invoker.invokeRestControllerAdapterMethod(
            match("echo", String.class), new FakeRestRequest("GET", "/echo")));
   }

   @Test
   @DisplayName("propagates JVM errors thrown by an interceptor")
   public void testInterceptorErrorPropagates()
   {
      RestControllerAdapterMethodInvoker invoker = invoker((invocation, chain) -> {
         throw new AssertionError("fatal");
      });

      assertThatExceptionOfType(AssertionError.class)
         .isThrownBy(() -> invoker.invokeRestControllerAdapterMethod(
            match("hello"), new FakeRestRequest("GET", "/hello")));
   }

   @Test
   @DisplayName("rejects a null interceptor list")
   public void testRejectsNullInterceptors()
   {
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new RestControllerAdapterMethodInvoker(
            JsonMapper.builder().build(), new DefaultFailureProblemMapper(), null));
   }

   private EndpointInterceptor recording(final String name)
   {
      return (invocation, chain) -> {
         calls.add(name + ":before");
         RestResponse response = chain.proceed(invocation);
         calls.add(name + ":after");
         return response;
      };
   }

   private static RestControllerAdapterMethodInvoker invoker(final EndpointInterceptor... interceptors)
   {
      return new RestControllerAdapterMethodInvoker(
         JsonMapper.builder().build(), new DefaultFailureProblemMapper(), List.of(interceptors));
   }

   private RouteMatch match(final String methodName, final Class<?>... parameterTypes)
   {
      try
      {
         return new RouteMatch(
            new HandlerMethod(adapter, Adapter.class.getMethod(methodName, parameterTypes)), Map.of());
      }
      catch (final NoSuchMethodException ex)
      {
         throw new IllegalStateException(ex);
      }
   }

   private static Object data(final RestResponse response)
   {
      assertThat(response.body()).containsInstanceOf(ApiResponse.class);
      return ((ApiResponse<?>) response.body().orElseThrow()).data().orElseThrow();
   }
}
