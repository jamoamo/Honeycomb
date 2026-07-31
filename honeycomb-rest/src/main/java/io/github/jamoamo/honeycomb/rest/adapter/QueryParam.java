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
package io.github.jamoamo.honeycomb.rest.adapter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a handler-method parameter to a query-string parameter of the request.
 *
 * <p>
 * The named query parameter is supplied to the annotated parameter, converted to the parameter's type.
 * When {@link #value()} is empty the parameter's own name is used as the query parameter name. A parameter
 * may be declared optional either by setting {@link #required()} to {@code false} or by supplying a
 * {@link #defaultValue()}; when a required parameter is absent request handling fails with a bad-request
 * response.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryParam
{
   /**
    * Sentinel indicating that no default value was supplied.
    */
   String NO_DEFAULT = "-honeycomb-no-default";

   /**
    * The name of the query parameter to bind. When empty the parameter's own name is used.
    *
    * @return the query parameter name
    */
   String value() default "";

   /**
    * Whether the query parameter must be present. When {@code true} and the parameter is absent without a
    * default value being supplied, request handling fails with a bad-request response.
    *
    * @return whether the parameter is required
    */
   boolean required() default true;

   /**
    * The value to use when the query parameter is absent. Supplying a default value implicitly makes the
    * parameter optional.
    *
    * @return the default value, or {@link #NO_DEFAULT} when none is supplied
    */
   String defaultValue() default NO_DEFAULT;
}
