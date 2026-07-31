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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * A simple in-memory {@link RestRequest} for tests.
 *
 * @author James Amoore
 */
public final class FakeRestRequest implements RestRequest
{
   private final String method;
   private final String path;
   private final Map<String, List<String>> queryParameters;
   private final String body;

   public FakeRestRequest(final String method, final String path)
   {
      this(method, path, Map.of(), "");
   }

   public FakeRestRequest(
      final String method,
      final String path,
      final Map<String, List<String>> queryParameters,
      final String body)
   {
      this.method = method;
      this.path = path;
      this.queryParameters = queryParameters;
      this.body = body;
   }

   @Override
   public String method()
   {
      return method;
   }

   @Override
   public String path()
   {
      return path;
   }

   @Override
   public List<String> queryParameterValues(final String name)
   {
      return queryParameters.getOrDefault(name, List.of());
   }

   @Override
   public InputStream body()
   {
      return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
   }
}
