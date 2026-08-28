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

import io.github.jamoamo.honeycomb.rest.spring.RestControllerAdapterHandlerMapping;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for the Honeycomb REST integration.
 *
 * @author James Amoore
 * @param enabled whether the Honeycomb REST handler mapping and handler adapter are registered; defaults to
 *        {@code true}
 * @param order   the order of the Honeycomb REST handler mapping among the application's handler mappings;
 *        defaults to {@link RestControllerAdapterHandlerMapping#DEFAULT_ORDER}. It must stay ahead of
 *        Spring's static resource mapping, which matches {@code /**} at
 *        {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE} {@code - 1}
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = HoneycombRestProperties.PREFIX)
public record HoneycombRestProperties(
   @DefaultValue("true") boolean enabled,
   @DefaultValue("1") int order)
{
   /**
    * The prefix of every property in this group.
    */
   public static final String PREFIX = "honeycomb.rest";
}
