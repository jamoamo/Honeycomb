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

import io.github.jamoamo.honeycomb.rest.BadRequestException;
import io.github.jamoamo.honeycomb.rest.RestControllerAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.rest.RestException;
import io.github.jamoamo.honeycomb.rest.RouteMatch;
import io.github.jamoamo.honeycomb.rest.response.ApiResponse;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.ObjectMapper;

/**
 * Handler adapter that services requests matched to a {@link RouteMatch} by a
 * {@link RestControllerAdapterHandlerMapping}.
 *
 * <p>
 * A successful invocation is written as a JSON {@link ApiResponse}. A binding failure is written as a
 * {@code 400} {@link ProblemDetail} and any other handling failure as a {@code 500} {@link ProblemDetail},
 * both with the {@code application/problem+json} media type, so that every endpoint surfaces errors in the
 * same shape.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class RestControllerAdapterHandlerAdapter implements HandlerAdapter
{
   private final RestControllerAdapterMethodInvoker invoker;
   private final ObjectMapper objectMapper;

   /**
    * Constructor.
    *
    * @param invoker      the method invoker
    * @param objectMapper the object mapper used to write responses
    */
   public RestControllerAdapterHandlerAdapter(
      final RestControllerAdapterMethodInvoker invoker,
      final ObjectMapper objectMapper)
   {
      this.invoker = invoker;
      this.objectMapper = objectMapper;
   }

   /**
    * Indicates if the handler is handled by this adapter.
    *
    * @param handler the handler
    */
   @Override
   public boolean supports(final Object handler)
   {
      return handler instanceof RouteMatch;
   }

   /**
    * Handles the request.
    *
    * @param req     the request
    * @param res     the response
    * @param handler the handler
    */
   @Override
   public ModelAndView handle(final HttpServletRequest req, final HttpServletResponse res, final Object handler)
      throws Exception
   {
      ServletRestRequest request = new ServletRestRequest(req);
      try
      {
         ApiResponse<?> result = invoker.invokeRestControllerAdapterMethod((RouteMatch) handler, request);

         res.setContentType(MediaType.APPLICATION_JSON_VALUE);
         objectMapper.writeValue(res.getOutputStream(), result);
      }
      catch (final BadRequestException ex)
      {
         writeProblemDetail(res, ProblemDetail.badRequest(
            ex.getMessage(), request.path(), ex.getClass().getSimpleName()));
      }
      catch (final RestException ex)
      {
         writeProblemDetail(res, ProblemDetail.internalServerError(
            ex.getMessage(), request.path(), ex.getClass().getSimpleName()));
      }
      return null;
   }

   private void writeProblemDetail(final HttpServletResponse res, final ProblemDetail detail) throws Exception
   {
      res.setStatus(detail.status());
      res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      objectMapper.writeValue(res.getOutputStream(), detail);
   }
}
