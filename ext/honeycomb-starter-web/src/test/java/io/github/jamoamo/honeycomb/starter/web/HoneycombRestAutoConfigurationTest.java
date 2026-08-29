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

import io.github.jamoamo.honeycomb.rest.PathTemplateParser;
import io.github.jamoamo.honeycomb.rest.RestControllerAdapterMethodInvoker;
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
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

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
