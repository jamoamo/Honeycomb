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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * A {@link RestRequest} whose body has been read into memory, so that it can be read any number of times; for
 * example by an interceptor that digests the body before the handler deserializes it.
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class BufferedRestRequest implements RestRequest
{
   private final RestRequest delegate;
   private final byte[] body;

   private BufferedRestRequest(final RestRequest delegate, final byte[] body)
   {
      this.delegate = delegate;
      this.body = body;
   }

   /**
    * Buffers a request's body, returning the request itself when it is already buffered.
    *
    * @param request the request
    * @return the buffered request
    * @throws IOException if the body cannot be read
    */
   public static BufferedRestRequest of(final RestRequest request) throws IOException
   {
      Objects.requireNonNull(request, "request cannot be null");
      if (request instanceof BufferedRestRequest buffered)
      {
         return buffered;
      }
      try (InputStream in = request.body())
      {
         return new BufferedRestRequest(request, in.readAllBytes());
      }
   }

   /**
    * The buffered body.
    *
    * @return a copy of the body bytes
    */
   public byte[] bodyBytes()
   {
      return body.clone();
   }

   @Override
   public String method()
   {
      return delegate.method();
   }

   @Override
   public String path()
   {
      return delegate.path();
   }

   @Override
   public List<String> queryParameterValues(final String name)
   {
      return delegate.queryParameterValues(name);
   }

   @Override
   public List<String> headerValues(final String name)
   {
      return delegate.headerValues(name);
   }

   @Override
   public InputStream body()
   {
      return new ByteArrayInputStream(body);
   }
}
