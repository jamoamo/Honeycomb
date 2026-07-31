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
package io.github.jamoamo.honeycomb.rest.argument;

import io.github.jamoamo.honeycomb.rest.request.RestRequest;

import java.util.Map;

/**
 * Per-request context passed to each {@link AdapterArgumentResolver}. Exposes the request together with the
 * path variables captured during route matching.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class ArgumentResolutionContext
{
   private final RestRequest request;
   private final Map<String, String> pathVariables;

   /**
    * Constructor.
    *
    * @param request       the current request
    * @param pathVariables the path variables captured during route matching, keyed by variable name
    */
   public ArgumentResolutionContext(final RestRequest request, final Map<String, String> pathVariables)
   {
      this.request = request;
      this.pathVariables = Map.copyOf(pathVariables);
   }

   /**
    * The current request.
    *
    * @return the request
    */
   public RestRequest request()
   {
      return request;
   }

   /**
    * The path variables captured during route matching, keyed by variable name.
    *
    * @return the captured path variables, or an empty map when none were captured
    */
   public Map<String, String> pathVariables()
   {
      return pathVariables;
   }
}
