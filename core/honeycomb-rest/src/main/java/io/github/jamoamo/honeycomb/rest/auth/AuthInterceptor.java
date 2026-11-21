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

import io.github.jamoamo.honeycomb.rest.intercept.EndpointInterceptor;
import io.github.jamoamo.honeycomb.rest.intercept.EndpointInvocation;
import io.github.jamoamo.honeycomb.rest.intercept.InterceptorChain;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * Authenticates and authorizes the caller of every endpoint that is not {@link PermitAll}.
 *
 * <p>
 * Endpoints are secured by default. For each secured invocation the credential is extracted, authenticated and
 * authorized, and the resulting {@link AuthenticatedPrincipal} is stored under
 * {@link AuthenticatedPrincipal#ATTRIBUTE} for later interceptors and {@link CurrentPrincipal} parameters. A
 * missing or rejected credential is answered with a {@code 401} and a denied caller with a {@code 403}, each
 * carrying an RFC 6750 {@code WWW-Authenticate} header; the reason is logged at debug level and never returned.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class AuthInterceptor implements EndpointInterceptor
{
   /**
    * The order at which the interceptor should run: ahead of any interceptor that relies on the principal.
    */
   public static final int ORDER = 1000;

   /**
    * The {@code WWW-Authenticate} header name.
    */
   public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

   private static final Logger LOGGER = LoggerFactory.getLogger(AuthInterceptor.class);
   private static final int UNAUTHORIZED = 401;
   private static final int FORBIDDEN = 403;

   private final CredentialExtractor extractor;
   private final TokenAuthenticator authenticator;
   private final AuthorizationPolicy policy;

   /**
    * Constructor using a {@link BearerTokenExtractor} and the {@link AnnotationAuthorizationPolicy}.
    *
    * @param authenticator the authenticator that validates bearer tokens
    */
   public AuthInterceptor(final TokenAuthenticator authenticator)
   {
      this(new BearerTokenExtractor(), authenticator, new AnnotationAuthorizationPolicy());
   }

   /**
    * Constructor.
    *
    * @param extractor     extracts the credential from the request
    * @param authenticator validates the credential
    * @param policy        decides whether the authenticated caller may invoke the endpoint
    */
   public AuthInterceptor(
      final CredentialExtractor extractor,
      final TokenAuthenticator authenticator,
      final AuthorizationPolicy policy)
   {
      this.extractor = Objects.requireNonNull(extractor, "extractor cannot be null");
      this.authenticator = Objects.requireNonNull(authenticator, "authenticator cannot be null");
      this.policy = Objects.requireNonNull(policy, "policy cannot be null");
   }

   @Override
   public RestResponse intercept(final EndpointInvocation invocation, final InterceptorChain chain)
   {
      if (isPermitAll(invocation.method()))
      {
         return chain.proceed(invocation);
      }
      return reject(invocation).orElseGet(() -> chain.proceed(invocation));
   }

   /**
    * Authenticates and authorizes the caller, storing the principal when both succeed.
    *
    * @param invocation the invocation
    * @return the rejection to answer with, or empty when the caller may proceed
    */
   private Optional<RestResponse> reject(final EndpointInvocation invocation)
   {
      RestResponse rejection = null;
      try
      {
         AuthenticatedPrincipal principal = authenticate(invocation);
         policy.authorize(principal, invocation);
         invocation.attributes().put(AuthenticatedPrincipal.ATTRIBUTE, principal);
      }
      catch (final MissingCredentialException ex)
      {
         rejection = unauthorized(invocation, "Bearer", "Authentication is required to access this resource.");
      }
      catch (final AuthenticationException ex)
      {
         LOGGER.debug("Rejected credential for {} {}: {}",
            invocation.request().method(), invocation.request().path(), ex.getMessage());
         rejection = unauthorized(invocation, "Bearer error=\"invalid_token\"", "The access token is invalid.");
      }
      catch (final AccessDeniedException ex)
      {
         LOGGER.debug("Denied access to {} {}: {}",
            invocation.request().method(), invocation.request().path(), ex.getMessage());
         rejection = RestResponse.problem(ProblemDetail.forStatus(FORBIDDEN, "Forbidden",
               "You do not have permission to access this resource.", invocation.request().path(), "FORBIDDEN"))
            .withHeader(WWW_AUTHENTICATE, "Bearer error=\"insufficient_scope\"");
      }
      return Optional.ofNullable(rejection);
   }

   private AuthenticatedPrincipal authenticate(final EndpointInvocation invocation)
   {
      String token = extractor.extract(invocation.request()).orElseThrow(MissingCredentialException::new);
      return Objects.requireNonNull(authenticator.authenticate(token), "TokenAuthenticator returned a null principal");
   }

   /**
    * Signals that the caller presented no credential at all.
    */
   private static final class MissingCredentialException extends AuthenticationException
   {
      private static final long serialVersionUID = 1L;

      MissingCredentialException()
      {
         super("No credential was presented");
      }
   }

   /**
    * Whether the handler method is public: it is when it declares {@link PermitAll}, or when its class does and the
    * method declares no requirement of its own.
    *
    * @param method the handler method
    * @return {@code true} when the endpoint is public
    */
   static boolean isPermitAll(final Method method)
   {
      if (method.isAnnotationPresent(PermitAll.class))
      {
         return true;
      }
      if (method.isAnnotationPresent(RequiresScopes.class) || method.isAnnotationPresent(RequiresRoles.class))
      {
         return false;
      }
      return method.getDeclaringClass().isAnnotationPresent(PermitAll.class);
   }

   private static RestResponse unauthorized(
      final EndpointInvocation invocation,
      final String challenge,
      final String detail)
   {
      return RestResponse.problem(ProblemDetail.forStatus(
            UNAUTHORIZED, "Unauthorized", detail, invocation.request().path(), "UNAUTHORIZED"))
         .withHeader(WWW_AUTHENTICATE, challenge);
   }
}
