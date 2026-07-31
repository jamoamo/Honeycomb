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
package io.github.jamoamo.honeycomb.rest.response;

import java.util.Objects;
import java.util.Optional;

/**
 * The envelope every successful API response is wrapped in, ensuring a consistent response shape across
 * endpoints.
 *
 * @author James Amoore
 * @param data the data
 * @param meta the metadata
 * @param <T>  the type of the data
 * @since 1.0.0
 */
public record ApiResponse<T>(Optional<T> data, Optional<ResponseMetaData> meta)
{
   /**
    * Constructor.
    */
   public ApiResponse
   {
      data = Objects.requireNonNull(data);
      meta = Objects.requireNonNull(meta);
   }

   /**
    * Creates a response carrying the given data and no metadata.
    *
    * @param data the response data, possibly null
    * @param <T>  the type of the data
    * @return the response
    */
   public static <T> ApiResponse<T> of(final T data)
   {
      return new ApiResponse<>(Optional.ofNullable(data), Optional.empty());
   }
}
