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

import io.github.jamoamo.honeycomb.adapter.argument.AdapterArgumentResolver;
import io.github.jamoamo.honeycomb.rest.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Parameter;

/**
 * Resolves a parameter by deserializing the request body into its type. This is the fallback resolver: it
 * supports any parameter and is therefore expected to be consulted last, so that annotated parameters are
 * resolved by their dedicated resolvers first.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class RequestBodyArgumentResolver implements AdapterArgumentResolver<ArgumentResolutionContext>
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RequestBodyArgumentResolver.class);

   private final ObjectMapper objectMapper;

   /**
    * Constructor.
    *
    * @param objectMapper the object mapper used to deserialize the request body
    */
   public RequestBodyArgumentResolver(final ObjectMapper objectMapper)
   {
      this.objectMapper = objectMapper;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean supports(final Parameter parameter)
   {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object resolve(final Parameter parameter, final ArgumentResolutionContext context) throws IOException
   {
      try
      {
         return objectMapper.readValue(context.request().body(), parameter.getType());
      }
      catch (final JacksonException ex)
      {
         LOGGER.warn("Failed to deserialize request body.", ex);
         throw new BadRequestException("Malformed request body.", ex);
      }
   }
}
