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
package io.github.jamoamo.honeycomb.rest.request;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A protocol-neutral view of an incoming REST request.
 *
 * <p>
 * This is the integration seam that keeps {@code honeycomb-rest} free of any particular HTTP runtime. A hosting
 * module (for example the Spring WebMVC integration) adapts its native request type to this interface, and
 * everything in the core - route matching, argument resolution, body deserialization - works against it.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public interface RestRequest
{
   /**
    * The HTTP method of the request, for example {@code GET}.
    *
    * @return the raw HTTP method name
    */
   String method();

   /**
    * The path of the request within the application, beginning with {@code /} and excluding any context path
    * and query string.
    *
    * @return the request path
    */
   String path();

   /**
    * The values supplied for the named query parameter.
    *
    * @param name the query parameter name
    * @return the values in the order supplied, or an empty list when the parameter is absent
    */
   List<String> queryParameterValues(String name);

   /**
    * The request body.
    *
    * @return a stream over the request body
    * @throws IOException if the body cannot be opened
    */
   InputStream body() throws IOException;
}
