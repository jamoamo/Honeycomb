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

import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ExceptionProblemDetailMapper}.
 *
 * @author James Amoore
 */
@DisplayName("ExceptionProblemDetailMapper")
public class ExceptionProblemDetailMapperTest
{
   private final Logger logger = mock(Logger.class);
   private final ExceptionProblemDetailMapper mapper = new ExceptionProblemDetailMapper(logger);
   private final FakeRestRequest request = new FakeRestRequest("GET", "/things");

   @Test
   @DisplayName("logs an unexpected exception at error with its cause and the error id it returns")
   public void testUnexpectedExceptionIsLoggedWithErrorId()
   {
      IllegalStateException cause = new IllegalStateException("kaboom");

      ProblemDetail problem = mapper.map(cause, request);

      verify(logger).error(anyString(), anyString(), eq(problem.errorId()), same(cause));
   }

   @Test
   @DisplayName("logs a carried server-error problem at error with the exception")
   public void testCarriedServerErrorIsLoggedAtError()
   {
      ProblemDetailException ex = new ProblemDetailException(
         ProblemDetail.forStatus(503, "Service Unavailable", "Down.", "/things", "DOWN"));

      assertThat(mapper.map(ex, request)).isSameAs(ex.problemDetail());

      verify(logger).error(anyString(), any(), any(), eq(503), eq("DOWN"), same(ex));
   }

   @Test
   @DisplayName("logs a carried client-error problem at warn without the exception")
   public void testCarriedClientErrorIsLoggedAtWarn()
   {
      ProblemDetailException ex = new ProblemDetailException(
         ProblemDetail.forStatus(409, "Conflict", "Exists.", "/things", "EXISTS"));

      mapper.map(ex, request);

      verify(logger).warn(anyString(), any(), any(), eq(409), eq("EXISTS"), eq("Exists."));
      verify(logger, never()).error(anyString(), any(Object[].class));
   }

   @Test
   @DisplayName("maps a null exception to a generic 500 rather than failing")
   public void testNullExceptionMapsTo500()
   {
      assertThat(mapper.map(null, request).status()).isEqualTo(500);
   }
}
