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
import io.github.jamoamo.honeycomb.rest.response.FieldError;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.types.ErrorCode;

import java.util.List;

/**
 * Maps a failure returned by an endpoint as an {@link EndpointResult.Failed} to the {@link ProblemDetail} the
 * response is written with.
 *
 * <p>
 * This is the single place that decides how error codes surface over HTTP: the status, title and client-facing
 * detail. {@link DefaultFailureProblemMapper} derives them from the error code's
 * {@link io.github.jamoamo.honeycomb.types.ErrorCategory ErrorCategory}; supply another implementation to the
 * {@link RestControllerAdapterMethodInvoker} to change that, for example to map specific error codes differently.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
@FunctionalInterface
public interface FailureProblemMapper
{
   /**
    * Maps the failure to a problem detail.
    *
    * @param errorCode the error code identifying the failure
    * @param errors    the field-level errors of the failure, empty unless it was a validation failure
    * @param request   the request being handled
    * @return the problem detail; never {@code null}
    */
   ProblemDetail map(ErrorCode errorCode, List<FieldError> errors, RestRequest request);
}
