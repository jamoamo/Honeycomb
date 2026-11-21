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

import io.github.jamoamo.honeycomb.adapter.argument.AdapterArgumentResolver;
import io.github.jamoamo.honeycomb.rest.argument.ArgumentResolutionContext;

import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Resolves {@link CurrentPrincipal} parameters from the principal stored by the {@link AuthInterceptor}.
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class CurrentPrincipalArgumentResolver implements AdapterArgumentResolver<ArgumentResolutionContext>
{
   @Override
   public boolean supports(final Parameter parameter)
   {
      return parameter.isAnnotationPresent(CurrentPrincipal.class);
   }

   @Override
   public Object resolve(final Parameter parameter, final ArgumentResolutionContext context)
   {
      Optional<AuthenticatedPrincipal> principal = context.attributes().get(AuthenticatedPrincipal.ATTRIBUTE);
      Class<?> type = parameter.getType();
      if (type == Optional.class)
      {
         return principal;
      }
      if (type != AuthenticatedPrincipal.class)
      {
         throw new IllegalStateException("@CurrentPrincipal parameter " + parameter + " must be declared as "
            + "AuthenticatedPrincipal or Optional<AuthenticatedPrincipal>");
      }
      return principal.orElseThrow(() -> new IllegalStateException("No authenticated principal for parameter "
         + parameter + "; a @PermitAll endpoint must declare it as Optional<AuthenticatedPrincipal>"));
   }
}
