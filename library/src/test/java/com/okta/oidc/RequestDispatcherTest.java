/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.okta.oidc;

import com.okta.oidc.util.DateUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class RequestDispatcherTest {
    private RequestDispatcher mDispatcher;
    private ExecutorService mCallbackExecutor;
    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mCallbackExecutor = Executors.newSingleThreadExecutor();
        mDispatcher = new RequestDispatcher(mCallbackExecutor);
    }

    @After
    public void tearDown() throws Exception {
        mCallbackExecutor.shutdown();
        mDispatcher.shutdown();
    }

    @Test
    public void shutdownNow() {
        mExpectedEx.expect(UnsupportedOperationException.class);
        mDispatcher.shutdownNow();
    }

    @Test
    public void isShutdown() {
        assertFalse(mDispatcher.isShutdown());
    }

    @Test
    public void isTerminated() {
        assertFalse(mDispatcher.isTerminated());
    }

    @Test
    public void awaitTermination() {
        assertFalse(mDispatcher.awaitTermination(DateUtil.getNow().getTime(),
                TimeUnit.MILLISECONDS));
    }

    @Test
    public void submitResults() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        long current_tid = Thread.currentThread().getId();
        final long[] dispatcher_tid = new long[1];
        final long[] callback_tid = new long[1];
        mDispatcher.execute(() -> {
            dispatcher_tid[0] = Thread.currentThread().getId();
        });
        mDispatcher.submitResults(() -> {
            callback_tid[0] = Thread.currentThread().getId();
            latch.countDown();
        });
        latch.await();
        assertNotEquals(dispatcher_tid[0], current_tid);
        assertNotEquals(callback_tid[0], current_tid);
        assertNotEquals(callback_tid[0], dispatcher_tid[0]);
    }

    @Test
    public void execute() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        long current_tid = Thread.currentThread().getId();
        final long[] dispatcher_tid = new long[1];
        mDispatcher.execute(() -> {
            dispatcher_tid[0] = Thread.currentThread().getId();
            latch.countDown();
        });
        latch.await();
        assertNotEquals(dispatcher_tid[0], current_tid);
    }

    @Test
    public void shutdown() {
        mDispatcher.shutdown();
        assertTrue(mDispatcher.isShutdown());
    }
}