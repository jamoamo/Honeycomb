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
package io.github.jamoamo.honeycomb.rest.auth;

import io.github.jamoamo.honeycomb.rest.DefaultFailureProblemMapper;
import io.github.jamoamo.honeycomb.rest.FakeRestRequest;
import io.github.jamoamo.honeycomb.rest.HandlerMethod;
import io.github.jamoamo.honeycomb.rest.RestControllerAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.rest.RouteMatch;
import io.github.jamoamo.honeycomb.rest.response.ApiResponse;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AuthInterceptor}, run through the {@link RestControllerAdapterMethodInvoker} together with the
 * default policy and {@link CurrentPrincipalArgumentResolver}.
 *
 * @author James Amoore
 */
@DisplayName("AuthInterceptor")
public class AuthInterceptorTest
{
   private static final AuthenticatedPrincipal JAMES = new AuthenticatedPrincipal(
      "james", Set.of("orders:read", "orders:write"), Set.of("admin"), Map.of("tenant", "t1"));

   private static final TokenAuthenticator AUTHENTICATOR = token -> switch (token)
   {
      case "good" -> JAMES;
      case "plain" -> AuthenticatedPrincipal.of("plain");
      case "null" -> null;
      default -> throw new AuthenticationException("Unknown token");
   };

   private final RestControllerAdapterMethodInvoker invoker = new RestControllerAdapterMethodInvoker(
      JsonMapper.builder().build(), new DefaultFailureProblemMapper(), List.of(new AuthInterceptor(AUTHENTICATOR)));

   public static class Adapter
   {
      public String secured(@CurrentPrincipal final AuthenticatedPrincipal principal)
      {
         return principal.subject();
      }

      @PermitAll
      public String open(@CurrentPrincipal final Optional<AuthenticatedPrincipal> principal)
      {
         return principal.map(AuthenticatedPrincipal::subject).orElse("anonymous");
      }

      @PermitAll
      public String openRequired(@CurrentPrincipal final AuthenticatedPrincipal principal)
      {
         return principal.subject();
      }

      @RequiresScopes({"orders:read", "orders:write"})
      public String allScopes()
      {
         return "ok";
      }

      @RequiresScopes(value = {"orders:delete", "orders:read"}, match = Match.ANY)
      public String anyScope()
      {
         return "ok";
      }

      @RequiresRoles({"auditor", "admin"})
      public String anyRole()
      {
         return "ok";
      }

      @RequiresRoles(value = {"auditor", "admin"}, match = Match.ALL)
      public String allRoles()
      {
         return "ok";
      }

      @RequiresScopes({})
      public String emptyRequirement()
      {
         return "ok";
      }

      public String badType(@CurrentPrincipal final String principal)
      {
         return principal;
      }
   }

   @PermitAll
   public static class PublicAdapter
   {
      public String inherited()
      {
         return "ok";
      }

      @RequiresScopes("orders:read")
      public String overridden()
      {
         return "ok";
      }
   }

   @RequiresScopes("admin:all")
   public static class ScopedAdapter
   {
      public String inherited()
      {
         return "ok";
      }

      @PermitAll
      public String open()
      {
         return "ok";
      }
   }

   @Test
   @DisplayName("answers a request without a credential with 401 and a bearer challenge")
   public void testMissingCredential()
   {
      RestResponse response = invoke(Adapter.class, "secured", null);

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.headers()).containsEntry(AuthInterceptor.WWW_AUTHENTICATE, List.of("Bearer"));
      assertThat(problem(response).detail()).isEqualTo("Authentication is required to access this resource.");
   }

   @Test
   @DisplayName("answers a rejected credential with 401 and invalid_token, without leaking the reason")
   public void testInvalidCredential()
   {
      RestResponse response = invoke(Adapter.class, "secured", "Bearer bad");

      assertThat(response.status()).isEqualTo(401);
      assertThat(response.headers())
         .containsEntry(AuthInterceptor.WWW_AUTHENTICATE, List.of("Bearer error=\"invalid_token\""));
      assertThat(problem(response).detail()).doesNotContain("Unknown token");
   }

   @Test
   @DisplayName("answers a malformed Authorization header with 401")
   public void testMalformedHeader()
   {
      assertThat(invoke(Adapter.class, "secured", "Basic abc").status()).isEqualTo(401);
   }

   @Test
   @DisplayName("answers an authenticator returning null with a 500")
   public void testNullPrincipal()
   {
      assertThat(invoke(Adapter.class, "secured", "Bearer null").status()).isEqualTo(500);
   }

   @Test
   @DisplayName("injects the authenticated principal")
   public void testInjectsPrincipal()
   {
      assertThat(data(invoke(Adapter.class, "secured", "Bearer good"))).isEqualTo("james");
   }

   @Test
   @DisplayName("invokes @PermitAll endpoints without authentication, ignoring any credential")
   public void testPermitAll()
   {
      assertThat(data(invoke(Adapter.class, "open", null))).isEqualTo("anonymous");
      assertThat(data(invoke(Adapter.class, "open", "Bearer bad"))).isEqualTo("anonymous");
   }

   @Test
   @DisplayName("answers a required principal on a @PermitAll endpoint with a 500")
   public void testRequiredPrincipalOnPermitAll()
   {
      assertThat(invoke(Adapter.class, "openRequired", null).status()).isEqualTo(500);
   }

   @Test
   @DisplayName("answers a @CurrentPrincipal parameter of the wrong type with a 500")
   public void testBadPrincipalType()
   {
      assertThat(invoke(Adapter.class, "badType", "Bearer good").status()).isEqualTo(500);
   }

   @Test
   @DisplayName("enforces scopes and roles with their match modes")
   public void testScopesAndRoles()
   {
      assertThat(invoke(Adapter.class, "allScopes", "Bearer good").status()).isEqualTo(200);
      assertThat(invoke(Adapter.class, "anyScope", "Bearer good").status()).isEqualTo(200);
      assertThat(invoke(Adapter.class, "anyRole", "Bearer good").status()).isEqualTo(200);
      assertThat(invoke(Adapter.class, "emptyRequirement", "Bearer plain").status()).isEqualTo(200);

      RestResponse denied = invoke(Adapter.class, "allRoles", "Bearer good");
      assertThat(denied.status()).isEqualTo(403);
      assertThat(denied.headers())
         .containsEntry(AuthInterceptor.WWW_AUTHENTICATE, List.of("Bearer error=\"insufficient_scope\""));
      assertThat(problem(denied).detail()).doesNotContain("auditor");

      assertThat(invoke(Adapter.class, "allScopes", "Bearer plain").status()).isEqualTo(403);
      assertThat(invoke(Adapter.class, "anyScope", "Bearer plain").status()).isEqualTo(403);
      assertThat(invoke(Adapter.class, "anyRole", "Bearer plain").status()).isEqualTo(403);
   }

   @Test
   @DisplayName("applies class-level @PermitAll unless the method declares a requirement")
   public void testClassLevelPermitAll()
   {
      assertThat(invoke(PublicAdapter.class, "inherited", null).status()).isEqualTo(200);
      assertThat(invoke(PublicAdapter.class, "overridden", null).status()).isEqualTo(401);
      assertThat(invoke(PublicAdapter.class, "overridden", "Bearer good").status()).isEqualTo(200);
   }

   @Test
   @DisplayName("applies class-level requirements unless the method is @PermitAll")
   public void testClassLevelRequirement()
   {
      assertThat(invoke(ScopedAdapter.class, "inherited", "Bearer good").status()).isEqualTo(403);
      assertThat(invoke(ScopedAdapter.class, "open", null).status()).isEqualTo(200);
   }

   @Test
   @DisplayName("uses a custom extractor and policy")
   public void testCustomCollaborators()
   {
      AuthInterceptor custom = new AuthInterceptor(
         request -> Optional.of("good"),
         AUTHENTICATOR,
         (principal, invocation) -> {
            throw new AccessDeniedException("nope");
         });
      RestControllerAdapterMethodInvoker customInvoker = new RestControllerAdapterMethodInvoker(
         JsonMapper.builder().build(), new DefaultFailureProblemMapper(), List.of(custom));

      assertThat(customInvoker.invokeRestControllerAdapterMethod(
         match(Adapter.class, "secured"), new FakeRestRequest("GET", "/x")).status()).isEqualTo(403);
   }

   @Test
   @DisplayName("rejects null collaborators")
   public void testRejectsNulls()
   {
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> new AuthInterceptor(null));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> new AuthInterceptor(
         null, AUTHENTICATOR, new AnnotationAuthorizationPolicy()));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> new AuthInterceptor(
         new BearerTokenExtractor(), AUTHENTICATOR, null));
   }

   private RestResponse invoke(final Class<?> type, final String method, final String authorization)
   {
      Map<String, List<String>> headers = authorization == null
         ? Map.of()
         : Map.of(BearerTokenExtractor.AUTHORIZATION, List.of(authorization));
      return invoker.invokeRestControllerAdapterMethod(
         match(type, method), new FakeRestRequest("GET", "/x", Map.of(), headers, ""));
   }

   private static RouteMatch match(final Class<?> type, final String name)
   {
      try
      {
         Object bean = type.getDeclaredConstructor().newInstance();
         return new RouteMatch(new HandlerMethod(bean, Arrays.stream(type.getMethods())
            .filter(m -> m.getName().equals(name)).findFirst().orElseThrow()), Map.of());
      }
      catch (final ReflectiveOperationException ex)
      {
         throw new IllegalStateException(ex);
      }
   }

   private static Object data(final RestResponse response)
   {
      assertThat(response.status()).isEqualTo(200);
      return ((ApiResponse<?>) response.body().orElseThrow()).data().orElseThrow();
   }

   private static ProblemDetail problem(final RestResponse response)
   {
      return (ProblemDetail) response.body().orElseThrow();
   }
}
