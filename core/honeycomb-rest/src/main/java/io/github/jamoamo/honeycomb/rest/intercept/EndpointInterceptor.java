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

import io.github.jamoamo.honeycomb.rest.response.RestResponse;

/**
 * Cross-cutting logic run around the invocation of every matched endpoint, for example authentication or
 * idempotency.
 *
 * <p>
 * Interceptors run in the order they are configured, each wrapping the next. An interceptor may short-circuit
 * by returning a response without calling {@link InterceptorChain#proceed(EndpointInvocation)}, or throw a
 * {@link io.github.jamoamo.honeycomb.rest.ProblemDetailException}, which is answered with the problem it
 * carries. Any other runtime exception is answered with a generic {@code 500}.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
@FunctionalInterface
public interface EndpointInterceptor
{
   /**
    * Intercepts an endpoint invocation.
    *
    * @param invocation the invocation
    * @param chain      the rest of the chain
    * @return the response
    */
   RestResponse intercept(EndpointInvocation invocation, InterceptorChain chain);
}
