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
import io.github.jamoamo.honeycomb.rest.HandlerMethod;
import io.github.jamoamo.honeycomb.rest.PathTemplateParser;
import io.github.jamoamo.honeycomb.rest.RestControllerAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.rest.RouteMatch;
import io.github.jamoamo.honeycomb.rest.intercept.EndpointInterceptor;
import io.github.jamoamo.honeycomb.rest.request.RestRequest;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;
import io.github.jamoamo.honeycomb.rest.spring.PathPatternTemplateParser;
import io.github.jamoamo.honeycomb.rest.spring.RestControllerAdapterHandlerAdapter;
import io.github.jamoamo.honeycomb.rest.spring.RestControllerAdapterHandlerMapping;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HoneycombRestAutoConfiguration}.
 *
 * @author James Amoore
 */
class HoneycombRestAutoConfigurationTest
{
   private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(HoneycombRestAutoConfiguration.class));

   @Test
   void registersTheHandlerMappingAndHandlerAdapterByDefault()
   {
      runner.run(context -> assertThat(context)
         .hasSingleBean(RestControllerAdapterHandlerMapping.class)
         .hasSingleBean(RestControllerAdapterHandlerAdapter.class)
         .hasSingleBean(RestControllerAdapterMethodInvoker.class)
         .hasSingleBean(PathTemplateParser.class)
         .hasSingleBean(ObjectMapper.class));
   }

   @Test
   void registersTheBeansWhenExplicitlyEnabled()
   {
      runner.withPropertyValues("honeycomb.rest.enabled=true")
         .run(context -> assertThat(context)
            .hasSingleBean(RestControllerAdapterHandlerMapping.class)
            .hasSingleBean(RestControllerAdapterHandlerAdapter.class));
   }

   @Test
   void registersNoBeansWhenDisabled()
   {
      runner.withPropertyValues("honeycomb.rest.enabled=false")
         .run(context -> assertThat(context)
            .doesNotHaveBean(RestControllerAdapterHandlerMapping.class)
            .doesNotHaveBean(RestControllerAdapterHandlerAdapter.class)
            .doesNotHaveBean(RestControllerAdapterMethodInvoker.class)
            .doesNotHaveBean(PathTemplateParser.class));
   }

   @Test
   void registersNoBeansOutsideOfAServletWebApplication()
   {
      new ApplicationContextRunner()
         .withConfiguration(AutoConfigurations.of(HoneycombRestAutoConfiguration.class))
         .run(context -> assertThat(context)
            .doesNotHaveBean(RestControllerAdapterHandlerMapping.class)
            .doesNotHaveBean(RestControllerAdapterHandlerAdapter.class));
   }

   @Test
   void backsOffWhenTheApplicationDeclaresItsOwnBeans()
   {
      runner.withUserConfiguration(CustomRestConfiguration.class)
         .run(context -> {
            assertThat(context).hasSingleBean(RestControllerAdapterHandlerMapping.class);
            assertThat(context).hasSingleBean(RestControllerAdapterHandlerAdapter.class);
            assertThat(context.getBean(RestControllerAdapterHandlerMapping.class))
               .isSameAs(context.getBean("customHandlerMapping"));
            assertThat(context.getBean(RestControllerAdapterHandlerAdapter.class))
               .isSameAs(context.getBean("customHandlerAdapter"));
            assertThat(context.getBean(ObjectMapper.class)).isSameAs(context.getBean("customObjectMapper"));
         });
   }

   @Test
   void registersTheDefaultFailureProblemMapper()
   {
      runner.run(context -> assertThat(context)
         .getBean(FailureProblemMapper.class)
         .isInstanceOf(DefaultFailureProblemMapper.class));
   }

   @Test
   void backsOffWhenTheApplicationDeclaresItsOwnFailureProblemMapper()
   {
      runner.withUserConfiguration(CustomFailureProblemMapperConfiguration.class)
         .run(context -> {
            assertThat(context).hasSingleBean(FailureProblemMapper.class);
            assertThat(context.getBean(FailureProblemMapper.class))
               .isSameAs(context.getBean("customFailureProblemMapper"));
            assertThat(context).hasSingleBean(RestControllerAdapterMethodInvoker.class);
         });
   }

   @Test
   void runsEndpointInterceptorBeansInOrder()
   {
      runner.withUserConfiguration(InterceptorConfiguration.class)
         .run(context -> {
            List<String> calls = context.getBean(InterceptorConfiguration.class).calls;
            RouteMatch match = new RouteMatch(
               new HandlerMethod(new Pinger(), Pinger.class.getMethod("ping")), Map.of());

            RestResponse response = context.getBean(RestControllerAdapterMethodInvoker.class)
               .invokeRestControllerAdapterMethod(match, new GetRequest());

            assertThat(response.status()).isEqualTo(200);
            assertThat(calls).containsExactly("first", "second", "handler");
         });
   }

   public static final class Pinger
   {
      public String ping()
      {
         return "pong";
      }
   }

   private static final class GetRequest implements RestRequest
   {
      @Override
      public String method()
      {
         return "GET";
      }

      @Override
      public String path()
      {
         return "/ping";
      }

      @Override
      public List<String> queryParameterValues(final String name)
      {
         return List.of();
      }

      @Override
      public InputStream body()
      {
         return InputStream.nullInputStream();
      }
   }

   @Configuration(proxyBeanMethods = false)
   static class InterceptorConfiguration
   {
      private final List<String> calls = new ArrayList<>();

      @Bean
      @Order(2)
      EndpointInterceptor secondInterceptor()
      {
         return (invocation, chain) -> {
            calls.add("second");
            RestResponse response = chain.proceed(invocation);
            calls.add("handler");
            return response;
         };
      }

      @Bean
      @Order(1)
      EndpointInterceptor firstInterceptor()
      {
         return (invocation, chain) -> {
            calls.add("first");
            return chain.proceed(invocation);
         };
      }
   }

   @Configuration(proxyBeanMethods = false)
   static class CustomFailureProblemMapperConfiguration
   {
      @Bean
      FailureProblemMapper customFailureProblemMapper()
      {
         return (code, errors, request) ->
            ProblemDetail.forStatus(418, "I'm a teapot", code.description(), request.path(), code.code());
      }
   }

   @Test
   void isRegisteredAsAnAutoConfiguration() throws Exception
   {
      ClassPathResource imports =
         new ClassPathResource("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

      assertThat(imports.getContentAsString(StandardCharsets.UTF_8).lines())
         .contains(HoneycombRestAutoConfiguration.class.getName());
   }

   @Test
   void bindsTheEnabledProperty()
   {
      runner.withPropertyValues("honeycomb.rest.enabled=true")
         .run(context -> assertThat(context.getBean(HoneycombRestProperties.class).enabled()).isTrue());
   }

   @Test
   void defaultsTheEnabledPropertyToTrue()
   {
      runner.run(context -> assertThat(context.getBean(HoneycombRestProperties.class).enabled()).isTrue());
   }

   @Test
   void ordersTheHandlerMappingAheadOfTheStaticResourceMapping()
   {
      runner.run(context -> assertThat(context.getBean(RestControllerAdapterHandlerMapping.class).getOrder())
         .isEqualTo(RestControllerAdapterHandlerMapping.DEFAULT_ORDER)
         .isLessThan(Ordered.LOWEST_PRECEDENCE - 1));
   }

   @Test
   void bindsTheOrderProperty()
   {
      runner.withPropertyValues("honeycomb.rest.order=-50")
         .run(context -> {
            assertThat(context.getBean(HoneycombRestProperties.class).order()).isEqualTo(-50);
            assertThat(context.getBean(RestControllerAdapterHandlerMapping.class).getOrder()).isEqualTo(-50);
         });
   }

   @Configuration(proxyBeanMethods = false)
   static class CustomRestConfiguration
   {
      @Bean
      ObjectMapper customObjectMapper()
      {
         return new ObjectMapper();
      }

      @Bean
      PathTemplateParser customPathTemplateParser()
      {
         return new PathPatternTemplateParser();
      }

      @Bean
      RestControllerAdapterMethodInvoker customInvoker(final ObjectMapper objectMapper)
      {
         return new RestControllerAdapterMethodInvoker(objectMapper);
      }

      @Bean
      RestControllerAdapterHandlerMapping customHandlerMapping(final PathTemplateParser parser)
      {
         return new RestControllerAdapterHandlerMapping(parser);
      }

      @Bean
      RestControllerAdapterHandlerAdapter customHandlerAdapter(
         final RestControllerAdapterMethodInvoker invoker,
         final ObjectMapper objectMapper)
      {
         return new RestControllerAdapterHandlerAdapter(invoker, objectMapper);
      }
   }
}
