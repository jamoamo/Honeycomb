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

import io.github.jamoamo.honeycomb.rest.response.FieldError;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.types.ErrorCategory;
import io.github.jamoamo.honeycomb.types.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link DefaultFailureProblemMapper}.
 *
 * @author James Amoore
 */
@DisplayName("DefaultFailureProblemMapper")
public class DefaultFailureProblemMapperTest
{
   private static final String PATH = "/things";

   private final Logger logger = mock(Logger.class);
   private final DefaultFailureProblemMapper mapper = new DefaultFailureProblemMapper(logger);
   private final FakeRestRequest request = new FakeRestRequest("POST", PATH);

   @ParameterizedTest
   @CsvSource({
      "VALIDATION, 400", "UNAUTHORISED, 401", "FORBIDDEN, 403", "NOT_FOUND, 404", "CONFLICT, 409",
      "PRECONDITION_FAILED, 412", "RATE_LIMITED, 429", "INTERNAL, 500", "UNAVAILABLE, 503", "TIMEOUT, 504"
   })
   @DisplayName("maps each error category to its HTTP status")
   public void testStatusForCategory(final ErrorCategory category, final int expected)
   {
      assertThat(DefaultFailureProblemMapper.status(category)).isEqualTo(expected);
      assertThat(mapper.map(new ErrorCode("C", category, "D."), List.of(), request).status()).isEqualTo(expected);
   }

   @Test
   @DisplayName("maps a client failure to a problem carrying its description, code and field errors")
   public void testClientFailure()
   {
      List<FieldError> errors = List.of(new FieldError("name", "is required"));

      ProblemDetail problem = mapper.map(
         new ErrorCode("INVALID", ErrorCategory.VALIDATION, "Rejected the input."), errors, request);

      assertThat(problem).isEqualTo(
         ProblemDetail.forStatus(400, "Bad Request", "Rejected the input.", PATH, "INVALID").withErrors(errors));
      verifyNoInteractions(logger);
   }

   @Test
   @DisplayName("hides the description of a server failure behind a generic detail and a logged error id")
   public void testServerFailureIsSanitisedAndLogged()
   {
      ProblemDetail problem = mapper.map(
         new ErrorCode("BOOM", ErrorCategory.INTERNAL, "Failed: internal secret"), List.of(), request);

      assertThat(problem.status()).isEqualTo(500);
      assertThat(problem.detail()).isEqualTo(DefaultFailureProblemMapper.INTERNAL_SERVER_ERROR_DETAIL);
      assertThat(problem.code()).isEqualTo("BOOM");
      assertThat(problem.instance()).isEqualTo(PATH);
      assertThat(problem.errorId()).isNotBlank();
      verify(logger).error(anyString(), any(), any(), eq("BOOM"), eq("Failed: internal secret"),
         eq(problem.errorId()));
   }
}
