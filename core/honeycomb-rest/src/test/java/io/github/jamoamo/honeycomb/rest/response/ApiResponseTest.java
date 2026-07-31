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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for {@link ApiResponse}.
 *
 * @author James Amoore
 */
@DisplayName("ApiResponse")
public class ApiResponseTest
{
   @Test
   @DisplayName("retains the data and metadata")
   public void testRetainsComponents()
   {
      ResponseMetaData meta = new ResponseMetaData(UUID.randomUUID(), Instant.now(), Instant.now());
      ApiResponse<String> response = new ApiResponse<>(Optional.of("data"), Optional.of(meta));

      assertThat(response.data()).contains("data");
      assertThat(response.meta()).contains(meta);
      assertThat(response.meta().get().requestId()).isEqualTo(meta.requestId());
      assertThat(response.meta().get().requestTimestamp()).isEqualTo(meta.requestTimestamp());
      assertThat(response.meta().get().responseTimestamp()).isEqualTo(meta.responseTimestamp());
   }

   @Test
   @DisplayName("rejects null components")
   public void testRejectsNullComponents()
   {
      assertThatNullPointerException()
         .isThrownBy(() -> new ApiResponse<>(null, Optional.empty()));
      assertThatNullPointerException()
         .isThrownBy(() -> new ApiResponse<>(Optional.empty(), null));
   }

   @Test
   @DisplayName("creates a response of nullable data")
   public void testOf()
   {
      assertThat(ApiResponse.of("data").data()).contains("data");
      assertThat(ApiResponse.of(null).data()).isEmpty();
   }
}
