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
package io.github.jamoamo.honeycomb.starter.web;

import io.github.jamoamo.honeycomb.rest.DefaultFailureProblemMapper;
import io.github.jamoamo.honeycomb.rest.FailureProblemMapper;
import io.github.jamoamo.honeycomb.rest.PathTemplateParser;
import io.github.jamoamo.honeycomb.rest.RestControllerAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.rest.auth.AnnotationAuthorizationPolicy;
import io.github.jamoamo.honeycomb.rest.auth.AuthInterceptor;
import io.github.jamoamo.honeycomb.rest.auth.AuthorizationPolicy;
import io.github.jamoamo.honeycomb.rest.auth.BearerTokenExtractor;
import io.github.jamoamo.honeycomb.rest.auth.CredentialExtractor;
import io.github.jamoamo.honeycomb.rest.auth.TokenAuthenticator;
import io.github.jamoamo.honeycomb.rest.intercept.EndpointInterceptor;
import io.github.jamoamo.honeycomb.rest.spring.PathPatternTemplateParser;
import io.github.jamoamo.honeycomb.rest.spring.RestControllerAdapterHandlerAdapter;
import io.github.jamoamo.honeycomb.rest.spring.RestControllerAdapterHandlerMapping;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.DispatcherServlet;
import tools.jackson.databind.ObjectMapper;

/**
 * Auto-configuration registering the Honeycomb Spring WebMVC REST integration.
 *
 * <p>
 * A {@link RestControllerAdapterHandlerMapping} and a {@link RestControllerAdapterHandlerAdapter} are
 * contributed to the application context, so that every bean annotated with
 * {@code @RestControllerAdapter} is routed to without any further wiring. Set
 * {@code honeycomb.rest.enabled=false} to register neither.
 * </p>
 *
 * <p>
 * Every bean is conditional on it being missing, so an application that needs different wiring can declare
 * its own bean of the same type and keep the rest of the auto-configuration.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnClass({RestControllerAdapterHandlerMapping.class, DispatcherServlet.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBooleanProperty(prefix = HoneycombRestProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({HoneycombRestProperties.class, HoneycombRestAuthProperties.class})
public class HoneycombRestAutoConfiguration
{
   /**
    * Secures every endpoint not marked {@code @PermitAll}, unless {@code honeycomb.rest.auth.enabled=false}.
    *
    * <p>
    * A {@link TokenAuthenticator} bean is required: without one the application fails to start rather than
    * silently serving unsecured endpoints.
    * </p>
    *
    * @author James Amoore
    * @since 1.1.0
    */
   @Configuration(proxyBeanMethods = false)
   @ConditionalOnBooleanProperty(prefix = HoneycombRestAuthProperties.PREFIX, name = "enabled", matchIfMissing = true)
   public static class AuthConfiguration
   {
      /**
       * The extractor of the caller's credential; a bearer token by default.
       *
       * @return the credential extractor
       */
      @Bean
      @ConditionalOnMissingBean
      public CredentialExtractor honeycombCredentialExtractor()
      {
         return new BearerTokenExtractor();
      }

      /**
       * The authorization policy; enforces {@code @RequiresScopes} and {@code @RequiresRoles} by default.
       *
       * @return the authorization policy
       */
      @Bean
      @ConditionalOnMissingBean
      public AuthorizationPolicy honeycombAuthorizationPolicy()
      {
         return new AnnotationAuthorizationPolicy();
      }

      /**
       * The interceptor that authenticates and authorizes callers.
       *
       * @param extractor      the credential extractor
       * @param authenticators the application's token authenticator
       * @param policy         the authorization policy
       * @return the interceptor
       * @throws IllegalStateException if the application declares no {@link TokenAuthenticator}
       */
      @Bean
      @ConditionalOnMissingBean
      @Order(AuthInterceptor.ORDER)
      public AuthInterceptor honeycombAuthInterceptor(
         final CredentialExtractor extractor,
         final ObjectProvider<TokenAuthenticator> authenticators,
         final AuthorizationPolicy policy)
      {
         TokenAuthenticator authenticator = authenticators.getIfUnique();
         if (authenticator == null)
         {
            throw new IllegalStateException("Honeycomb REST endpoints are secured by default but no single "
               + "TokenAuthenticator bean is defined. Declare one (for example with honeycomb-auth-jwt), or set "
               + HoneycombRestAuthProperties.PREFIX + ".enabled=false to serve endpoints without authentication.");
         }
         return new AuthInterceptor(extractor, authenticator, policy);
      }
   }

   /**
    * The object mapper used to read request bodies and write responses, for an application that does not
    * already have one. Spring Boot's own Jackson auto-configuration contributes one to every typical
    * application, and this configuration is ordered after it.
    *
    * @return a default object mapper
    */
   @Bean
   @ConditionalOnMissingBean
   public ObjectMapper objectMapper()
   {
      return new ObjectMapper();
   }

   /**
    * The parser used to compile endpoint route templates.
    *
    * @return a parser backed by Spring's path pattern parser
    */
   @Bean
   @ConditionalOnMissingBean
   public PathTemplateParser honeycombPathTemplateParser()
   {
      return new PathPatternTemplateParser();
   }

   /**
    * The policy that maps failed endpoint results to problem details; declare a {@link FailureProblemMapper} bean
    * to change how error codes surface over HTTP.
    *
    * @return the failure problem mapper
    */
   @Bean
   @ConditionalOnMissingBean
   public FailureProblemMapper honeycombFailureProblemMapper()
   {
      return new DefaultFailureProblemMapper();
   }

   /**
    * The invoker that binds request values to handler-method parameters and invokes them.
    *
    * @param objectMapper  the object mapper used to deserialize request bodies
    * @param failureMapper the policy that maps failed endpoint results to problem details
    * @param interceptors  every {@link EndpointInterceptor} bean, run around each endpoint in {@code @Order}
    *        order, lowest first and outermost
    * @return the method invoker
    */
   @Bean
   @ConditionalOnMissingBean
   public RestControllerAdapterMethodInvoker honeycombRestControllerAdapterMethodInvoker(
      final ObjectMapper objectMapper,
      final FailureProblemMapper failureMapper,
      final ObjectProvider<EndpointInterceptor> interceptors)
   {
      return new RestControllerAdapterMethodInvoker(
         objectMapper, failureMapper, interceptors.orderedStream().toList());
   }

   /**
    * The handler mapping that registers the routes of every {@code @RestControllerAdapter} bean.
    *
    * @param pathTemplateParser the parser used to compile endpoint route templates
    * @param properties         the REST integration properties, supplying the mapping's order
    * @return the handler mapping
    */
   @Bean
   @ConditionalOnMissingBean
   public RestControllerAdapterHandlerMapping restControllerAdapterHandlerMapping(
      final PathTemplateParser pathTemplateParser,
      final HoneycombRestProperties properties)
   {
      RestControllerAdapterHandlerMapping mapping = new RestControllerAdapterHandlerMapping(pathTemplateParser);
      mapping.setOrder(properties.order());
      return mapping;
   }

   /**
    * The handler adapter that services requests matched by the handler mapping.
    *
    * @param invoker      the method invoker
    * @param objectMapper the object mapper used to write responses
    * @return the handler adapter
    */
   @Bean
   @ConditionalOnMissingBean
   public RestControllerAdapterHandlerAdapter restControllerAdapterHandlerAdapter(
      final RestControllerAdapterMethodInvoker invoker,
      final ObjectMapper objectMapper)
   {
      return new RestControllerAdapterHandlerAdapter(invoker, objectMapper);
   }
}
