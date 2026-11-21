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

import io.github.jamoamo.honeycomb.rest.intercept.EndpointInvocation;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * The default {@link AuthorizationPolicy}: enforces {@link RequiresScopes} and {@link RequiresRoles}, each looked up
 * on the handler method and then on its adapter class. An endpoint with neither is open to every authenticated
 * caller.
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class AnnotationAuthorizationPolicy implements AuthorizationPolicy
{
   @Override
   public void authorize(final AuthenticatedPrincipal principal, final EndpointInvocation invocation)
   {
      invocation.findAnnotation(RequiresScopes.class).ifPresent(required ->
         check(required.value(), required.match(), principal::hasScope, "scopes", principal));
      invocation.findAnnotation(RequiresRoles.class).ifPresent(required ->
         check(required.value(), required.match(), principal::hasRole, "roles", principal));
   }

   private static void check(
      final String[] required,
      final Match match,
      final Predicate<String> granted,
      final String kind,
      final AuthenticatedPrincipal principal)
   {
      if (required.length == 0)
      {
         return;
      }
      boolean allowed = match == Match.ALL
         ? Arrays.stream(required).allMatch(granted)
         : Arrays.stream(required).anyMatch(granted);
      if (!allowed)
      {
         throw new AccessDeniedException("Principal " + principal.subject() + " lacks the required " + kind + " ("
            + match + " of " + Arrays.toString(required) + ")");
      }
   }
}
