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
package io.github.jamoamo.honeycomb.rest.intercept;

import io.github.jamoamo.honeycomb.rest.RouteMatch;
import io.github.jamoamo.honeycomb.rest.request.RestRequest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * The invocation of a matched endpoint, as seen by an {@link EndpointInterceptor}.
 *
 * @author James Amoore
 * @param request    the request being handled
 * @param match      the matched route
 * @param attributes the per-request attributes, shared with later interceptors and argument resolvers
 * @since 1.1.0
 */
public record EndpointInvocation(RestRequest request, RouteMatch match, Attributes attributes)
{
   /**
    * Constructor.
    *
    * @param request    the request being handled
    * @param match      the matched route
    * @param attributes the per-request attributes
    */
   public EndpointInvocation
   {
      Objects.requireNonNull(request, "request cannot be null");
      Objects.requireNonNull(match, "match cannot be null");
      Objects.requireNonNull(attributes, "attributes cannot be null");
   }

   /**
    * The handler method being invoked.
    *
    * @return the handler method
    */
   public Method method()
   {
      return match.handler().method();
   }

   /**
    * Finds an annotation on the handler method, falling back to the class that declares it, so that an
    * annotation on an adapter class applies to every endpoint that does not declare its own.
    *
    * @param annotationType the annotation type
    * @param <A>            the annotation type
    * @return the annotation, or empty when neither the method nor its class declares it
    */
   public <A extends Annotation> Optional<A> findAnnotation(final Class<A> annotationType)
   {
      Method method = method();
      A annotation = method.getAnnotation(annotationType);
      if (annotation == null)
      {
         annotation = method.getDeclaringClass().getAnnotation(annotationType);
      }
      return Optional.ofNullable(annotation);
   }
}
