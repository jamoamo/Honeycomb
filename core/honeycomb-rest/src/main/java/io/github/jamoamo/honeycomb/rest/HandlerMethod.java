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

import io.github.jamoamo.honeycomb.usecase.UseCaseResult;

import java.lang.reflect.Method;

/**
 * Represents a method that handles a request.
 *
 * <p>
 * A handler method may not declare a {@link UseCaseResult} return type: a use case result belongs to the
 * application layer, so endpoints convert it to an {@link EndpointResult} instead. Rejecting such a method here
 * surfaces the mistake when routes are registered rather than when a request is first handled.
 * </p>
 *
 * @author James Amoore
 * @param bean   the adapter instance the method belongs to
 * @param method the handling method
 * @since 1.0.0
 */
public record HandlerMethod(Object bean, Method method)
{
   /**
    * Constructor.
    *
    * @throws IllegalArgumentException if the method declares a {@link UseCaseResult} return type
    */
   public HandlerMethod
   {
      if (method != null && UseCaseResult.class.isAssignableFrom(method.getReturnType()))
      {
         throw new IllegalArgumentException("Handler method " + method + " returns a UseCaseResult; "
            + "convert it with EndpointResult.from(...) or EndpointResult.respond(...) and return the "
            + "EndpointResult instead");
      }
   }
}
