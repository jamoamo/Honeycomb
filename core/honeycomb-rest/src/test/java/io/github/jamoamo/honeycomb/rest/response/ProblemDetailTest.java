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
package io.github.jamoamo.honeycomb.rest.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProblemDetail}.
 *
 * @author James Amoore
 */
@DisplayName("ProblemDetail")
public class ProblemDetailTest
{
   @Test
   @DisplayName("creates a 400 Bad Request problem")
   public void testBadRequest()
   {
      ProblemDetail problem = ProblemDetail.badRequest("missing parameter", "/users", "BadRequestException");

      assertThat(problem.type()).isEqualTo(ProblemDetail.BLANK_TYPE);
      assertThat(problem.title()).isEqualTo("Bad Request");
      assertThat(problem.status()).isEqualTo(400);
      assertThat(problem.detail()).isEqualTo("missing parameter");
      assertThat(problem.instance()).isEqualTo("/users");
      assertThat(problem.code()).isEqualTo("BadRequestException");
   }

   @Test
   @DisplayName("creates a 500 Internal Server Error problem")
   public void testInternalServerError()
   {
      ProblemDetail problem = ProblemDetail.internalServerError("it broke", "/users", "RestException");

      assertThat(problem.title()).isEqualTo("Internal Server Error");
      assertThat(problem.status()).isEqualTo(500);
      assertThat(problem.detail()).isEqualTo("it broke");
   }

   @Test
   @DisplayName("creates a problem for an arbitrary status")
   public void testForStatus()
   {
      ProblemDetail problem = ProblemDetail.forStatus(404, "Not Found", "no user", "/users/9", "USER_NOT_FOUND");

      assertThat(problem.type()).isEqualTo(ProblemDetail.BLANK_TYPE);
      assertThat(problem.title()).isEqualTo("Not Found");
      assertThat(problem.status()).isEqualTo(404);
      assertThat(problem.detail()).isEqualTo("no user");
      assertThat(problem.instance()).isEqualTo("/users/9");
      assertThat(problem.code()).isEqualTo("USER_NOT_FOUND");
   }
}
