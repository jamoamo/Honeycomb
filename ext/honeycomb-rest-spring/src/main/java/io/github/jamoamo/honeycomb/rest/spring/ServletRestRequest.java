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
package io.github.jamoamo.honeycomb.rest.spring;

import io.github.jamoamo.honeycomb.rest.request.RestRequest;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A {@link RestRequest} view over an {@link HttpServletRequest}.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class ServletRestRequest implements RestRequest
{
   private final HttpServletRequest request;

   /**
    * Constructor.
    *
    * @param request the servlet request to adapt
    */
   public ServletRestRequest(final HttpServletRequest request)
   {
      this.request = request;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String method()
   {
      return request.getMethod();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String path()
   {
      String path = request.getRequestURI().substring(request.getContextPath().length());
      if (path.isEmpty())
      {
         return "/";
      }

      return path;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<String> queryParameterValues(final String name)
   {
      String[] values = request.getParameterValues(name);
      if (values == null)
      {
         return List.of();
      }

      return List.of(values);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InputStream body() throws IOException
   {
      return request.getInputStream();
   }
}
