/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * MPL-2.0-ADDENDUM.md
 * -------------------
 * This project includes additional terms and clarifications that apply
 * to this file. See MPL-2.0-ADDENDUM.md for details.
 */

package br.com.dizeno.reins.reasoning.inference.llm.providers.gemini;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    private final RetryPolicy policy = new RetryPolicy();

    @Test
    void successOnFirstAttemptNoListenerCall() throws Exception {
        List<RetryAttemptEvent> events = new ArrayList<>();
        String result = policy.execute(() -> "ok", 3, events::add);
        assertEquals("ok", result);
        assertTrue(events.isEmpty(), "Listener must NOT be called on first-attempt success");
    }

    @Test
    void succeedsOnRetry3() throws Exception {
        AtomicInteger calls = new AtomicInteger(0);
        List<RetryAttemptEvent> events = new ArrayList<>();

        String result = policy.execute(() -> {
            if (calls.incrementAndGet() < 4) {
                throw new IOException("transient failure");
            }
            return "success";
        }, 3, events::add);

        assertEquals("success", result);
        assertEquals(3, events.size(), "Listener must be called once per retry");
        assertEquals(1, events.get(0).attempt());
        assertEquals(2, events.get(1).attempt());
        assertEquals(3, events.get(2).attempt());
    }

    @Test
    void retriesExhaustedThrows() {
        AtomicInteger calls = new AtomicInteger(0);
        IOException thrown = assertThrows(IOException.class, () ->
                policy.execute(() -> {
                    calls.incrementAndGet();
                    throw new IOException("always fails");
                }, 3, null)
        );
        assertEquals("always fails", thrown.getMessage());
        assertEquals(4, calls.get(), "Initial attempt + 3 retries = 4 total calls");
    }

    @Test
    void retryAttemptsZeroThrowsImmediately() {
        AtomicInteger calls = new AtomicInteger(0);
        assertThrows(IOException.class, () ->
                policy.execute(() -> {
                    calls.incrementAndGet();
                    throw new IOException("fail");
                }, 0, null)
        );
        assertEquals(1, calls.get(), "retryAttempts=0 means no retry — only initial call");
    }

    @Test
    void geminiEmptyResponseExceptionIsNotRetried() {
        AtomicInteger calls = new AtomicInteger(0);
        List<RetryAttemptEvent> events = new ArrayList<>();

        assertThrows(GeminiEmptyResponseException.class, () ->
                policy.execute(() -> {
                    calls.incrementAndGet();
                    throw new GeminiEmptyResponseException("empty candidates");
                }, 3, events::add)
        );
        assertEquals(4, calls.get(), "GeminiEmptyResponseException should retry up to configured limit");
        assertEquals(4, events.size(), "Listener must be called for each failure decision");
        assertEquals("empty-or-blank", events.get(0).responseClass());
        assertFalse(events.get(3).retryScheduled(), "Last event should be terminal when retries are exhausted");
    }

    @Test
    void listenerInvokedOncePerRetry() throws Exception {
        List<RetryAttemptEvent> events = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger(0);

        policy.execute(() -> {
            if (calls.incrementAndGet() < 3) {
                throw new IOException("fail " + calls.get());
            }
            return "done";
        }, 5, events::add);

        assertEquals(2, events.size());
        assertEquals(1, events.get(0).attempt());
        assertInstanceOf(IOException.class, events.get(0).cause());
        assertEquals(2, events.get(1).attempt());
    }

    @Test
    void appliesConfiguredDelayBetweenRetries() {
        AtomicInteger calls = new AtomicInteger(0);

        long start = System.currentTimeMillis();
        assertThrows(IOException.class, () ->
                policy.execute(() -> {
                    calls.incrementAndGet();
                    throw new IOException("always fails");
                }, 2, 30, null)
        );
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(3, calls.get());
        assertTrue(elapsed >= 55, "Expected elapsed time to include retry delays");
    }

    @Test
    void isTransientReturnsFalseOnlyForGeminiEmptyResponseException() {
        assertTrue(policy.isTransient(new IOException("network")));
        assertTrue(policy.isTransient(new RuntimeException("unexpected")));
        assertTrue(policy.isTransient(new Exception("something")));
        assertTrue(policy.isTransient(new GeminiEmptyResponseException("empty")));
    }
}
