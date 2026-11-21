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

import io.github.jamoamo.honeycomb.rest.intercept.EndpointInvocation;

/**
 * Decides whether an authenticated caller may invoke an endpoint.
 *
 * @author James Amoore
 * @since 1.1.0
 */
@FunctionalInterface
public interface AuthorizationPolicy
{
   /**
    * Authorizes an invocation.
    *
    * @param principal  the authenticated caller
    * @param invocation the endpoint invocation, exposing the handler method and its annotations
    * @throws AccessDeniedException if the caller may not invoke the endpoint
    */
   void authorize(AuthenticatedPrincipal principal, EndpointInvocation invocation);
}
