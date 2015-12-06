/*  Copyright 2015 Hayai Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.seizonsenryaku.hayailauncher.threading;

import java.util.concurrent.LinkedBlockingQueue;


public class SimpleTaskConsumerManager {

    private final LinkedBlockingQueue<Task> mTasks;
    private volatile int mNumThreadsAlive;
    private boolean mConsumersShouldDie;
    private SimpleTaskConsumer[] mSimpleTaskConsumers;

    public SimpleTaskConsumerManager(final int numConsumers) {
        mTasks = new LinkedBlockingQueue<>();
        startConsumers(numConsumers);

    }

    private void startConsumers(final int numConsumers) {
        mSimpleTaskConsumers = new SimpleTaskConsumer[numConsumers];
        for (int i = 0; i < numConsumers; i++) {
            mSimpleTaskConsumers[i] = new SimpleTaskConsumer();
            Thread thread = new Thread(mSimpleTaskConsumers[i]);
            thread.start();
        }
    }

    public void addTask(final Task task) {
        if (mConsumersShouldDie) return; //TODO throw exception

        try {
            mTasks.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void removeAllTasks() {
        mTasks.clear();
    }

    public void destroyAllConsumers(boolean finishCurrentTasks) {
        if (mConsumersShouldDie) return;

        mConsumersShouldDie = true;
        if (!finishCurrentTasks) removeAllTasks();
        int threadsToKill = mNumThreadsAlive;
        DieTask dieTask = new DieTask();
        for (int i = 0; i < threadsToKill; i++) {
            mTasks.add(dieTask);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        //make sure the threads are properly killed
        destroyAllConsumers(false);

        super.finalize();
    }

    public static abstract class Task {
        public abstract void doTask();
    }

    //Dummy task, does nothing. Used to properly wake the threads to kill them.
    public static class DieTask extends Task {
        public void doTask() {
            //nothing here.
        }
    }

    private class SimpleTaskConsumer implements Runnable {

        @Override
        public void run() {
            do {
                try {
                    final Task task = mTasks.take();
                    task.doTask();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } while (!mConsumersShouldDie);
            mNumThreadsAlive--;

        }
    }
}
