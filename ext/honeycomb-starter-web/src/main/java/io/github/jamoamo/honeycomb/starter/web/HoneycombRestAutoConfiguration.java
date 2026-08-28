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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
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
@EnableConfigurationProperties(HoneycombRestProperties.class)
public class HoneycombRestAutoConfiguration
{
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
    * The invoker that binds request values to handler-method parameters and invokes them.
    *
    * @param objectMapper the object mapper used to deserialize request bodies
    * @return the method invoker
    */
   @Bean
   @ConditionalOnMissingBean
   public RestControllerAdapterMethodInvoker honeycombRestControllerAdapterMethodInvoker(
      final ObjectMapper objectMapper)
   {
      return new RestControllerAdapterMethodInvoker(objectMapper);
   }

   /**
    * The handler mapping that registers the routes of every {@code @RestControllerAdapter} bean.
    *
    * @param pathTemplateParser the parser used to compile endpoint route templates
    * @return the handler mapping
    */
   @Bean
   @ConditionalOnMissingBean
   public RestControllerAdapterHandlerMapping restControllerAdapterHandlerMapping(
      final PathTemplateParser pathTemplateParser)
   {
      return new RestControllerAdapterHandlerMapping(pathTemplateParser);
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
