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

import io.github.jamoamo.honeycomb.rest.request.RestRequest;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;
import io.github.jamoamo.honeycomb.usecase.UseCaseResult;

/**
 * Maps a {@link UseCaseResult} returned by a rest controller adapter method to a {@link RestResponse}, using the
 * standard mapping in {@link UseCaseResponses}.
 *
 * @author James Amoore
 * @since 1.1.0
 */
final class UseCaseResultMapper
{
   /**
    * Maps the use case result.
    *
    * @param result  the result to map
    * @param request the request being handled
    * @return the response
    */
   RestResponse map(final UseCaseResult<?> result, final RestRequest request)
   {
      return UseCaseResponses.toRestResponse(result, request.path(), request.method() + " " + request.path());
   }
}
