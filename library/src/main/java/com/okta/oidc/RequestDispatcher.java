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

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RestrictTo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * @hide Executor Service that runs tasks on worker thread
 * call back on ui thread or specified executor.
 */
@RestrictTo(LIBRARY_GROUP)
public class RequestDispatcher extends AbstractExecutorService {
    private static final int MAX_THREADS = 3;
    private boolean mShutdown = false;
    //executor used to run async network requests. only single thread
    private ExecutorService mExecutorService;

    //executor used to run async requests not related to networking.
    private static final ExecutorService sTaskExecutor =
            Executors.newFixedThreadPool(MAX_THREADS);

    //callback executor provide by app for callbacks
    private Executor mCallbackExecutor;

    //main handler for callbacks on main thread.
    private Handler mHandler;

    private Set<Future> mExecutorServiceTasks;

    private synchronized ExecutorService getExecutorService() {
        if (mExecutorService == null) {
            mExecutorService = Executors.newSingleThreadExecutor();
        }
        return mExecutorService;
    }

    public RequestDispatcher(Executor callbackExecutor) {
        mExecutorServiceTasks = new HashSet<>();
        if (callbackExecutor == null) {
            mHandler = new Handler(Looper.getMainLooper());
        } else {
            mCallbackExecutor = callbackExecutor;
        }
    }

    @Override
    public void shutdown() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mCallbackExecutor instanceof ExecutorService) {
            ((ExecutorService) mCallbackExecutor).shutdown();
        }
        if (mExecutorService != null) {
            mExecutorService.shutdown();
        }
        mShutdown = true;
    }

    public void stopAllTasks() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }

        Iterator<Future> executorIterator = mExecutorServiceTasks.iterator();
        while (executorIterator.hasNext()) {
            executorIterator.next().cancel(true);
            executorIterator.remove();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return mShutdown;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return false;
    }

    public void submitResults(Runnable command) {
        //run callbacks on provided executor or ui thread.
        if (mCallbackExecutor != null) {
            mCallbackExecutor.execute(command);
        } else if (mHandler.getLooper() == Looper.myLooper()) {
            command.run();
        } else {
            mHandler.post(command);
        }
    }

    @Override
    public void execute(Runnable command) {
        mExecutorServiceTasks.add(getExecutorService().submit(command));
    }

    public void runTask(Runnable runnable) {
        mExecutorServiceTasks.add(sTaskExecutor.submit(runnable));
    }

    //Debugging
    public static String createStackElementTagFor(Thread thread) {
        StackTraceElement[] elements = thread.getStackTrace();
        StringBuilder trace = new StringBuilder();
        for (StackTraceElement element : elements) {
            trace.append(String.format("(%s:%s)#%s",
                    element.getFileName(),
                    element.getLineNumber(),
                    element.getMethodName())).append("\n");
        }
        return trace.toString();
    }
}
