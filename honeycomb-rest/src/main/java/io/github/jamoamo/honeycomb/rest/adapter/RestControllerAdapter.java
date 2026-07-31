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
 * Declares a class as a rest controller adapter - an incoming boundary whose {@link Endpoint} methods handle
 * REST requests.
 *
 * <p>
 * The paths declared by the class's endpoints are registered under a base path derived from this annotation:
 * when {@link #fullPath()} is supplied it is used verbatim; otherwise, when {@link #version()} is supplied the
 * base path is {@code /api/{version}{apiPath}}; otherwise the base path is {@link #apiPath()} alone.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestControllerAdapter
{
   /**
    * The api version.
    *
    * @return the version
    */
   String version() default "";

   /**
    * The path for the controller, excluding the {@code /api/{version}} prefix.
    *
    * @return the api path
    */
   String apiPath() default "";

   /**
    * The full path for the controller, replacing the {@code /api/{version}{apiPath}} path.
    *
    * @return the full api path
    */
   String fullPath() default "";
}
