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
package io.github.jamoamo.honeycomb.telegram.argument;

import io.github.jamoamo.honeycomb.adapter.argument.AdapterArgumentResolver;
import io.github.jamoamo.honeycomb.telegram.update.BotUpdate;

import java.lang.reflect.Parameter;

/**
 * Resolves a parameter typed as {@link BotUpdate} to the update being handled, so that a handler needing the
 * raw text or update type can ask for it.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class BotUpdateArgumentResolver implements AdapterArgumentResolver<BotUpdate>
{
   /**
    * {@inheritDoc}
    */
   @Override
   public boolean supports(final Parameter parameter)
   {
      return parameter.getType() == BotUpdate.class;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object resolve(final Parameter parameter, final BotUpdate update)
   {
      return update;
   }
}
