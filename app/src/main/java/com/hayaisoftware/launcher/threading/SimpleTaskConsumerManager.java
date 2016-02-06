/* Copyright 2015 Hayai Software
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

package com.hayaisoftware.launcher.threading;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class SimpleTaskConsumerManager {

    private final BlockingQueue<Task> mTasks;
    private volatile int mNumThreadsAlive;
    private boolean mConsumersShouldDie;
    private SimpleTaskConsumer[] mSimpleTaskConsumers;
    private Thread[] threads;

    public SimpleTaskConsumerManager(final int numConsumers, final int queueSize) {
        if(queueSize<1)
            mTasks = new LinkedBlockingQueue<>();
        else
            mTasks = new ArrayBlockingQueue<>(queueSize);
        startConsumers(numConsumers);

    }

    public SimpleTaskConsumerManager(final int numConsumers) {
        mTasks = new LinkedBlockingQueue<>();
        startConsumers(numConsumers);

    }

    private void startConsumers(final int numConsumers) {
        mSimpleTaskConsumers = new SimpleTaskConsumer[numConsumers];
        threads=new Thread[numConsumers];
        for (int i = 0; i < numConsumers; i++) {
            mSimpleTaskConsumers[i] = new SimpleTaskConsumer();
            threads[i] = new Thread(mSimpleTaskConsumers[i]);
            threads[i].start();
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

    public void destroyAllConsumers(final boolean finishCurrentTasks) {
        destroyAllConsumers(finishCurrentTasks, false);
    }


    public void destroyAllConsumers(final boolean finishCurrentTasks,
                                    final boolean blockUntilFinished) {
        if (mConsumersShouldDie) return;
        mConsumersShouldDie = true;

        if (!finishCurrentTasks) removeAllTasks();


        final DieTask dieTask = new DieTask();
        for(final Thread thread:threads){
            try {
                mTasks.put(dieTask);
                //Log.d("Multithread", "Added DieTask");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //Log.d("Multithread","Added All DieTasks");
        if (blockUntilFinished) {

            for(final Thread thread:threads){
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    @Override
    protected void finalize() throws Throwable {
        //make sure the threads are properly killed
        destroyAllConsumers(false);

        super.finalize();
    }

    public static abstract class Task {

        //Returns true if you want the thread that run the task to continue running.
        public abstract boolean doTask();
    }

    //Dummy task, does nothing. Used to properly wake the threads to kill them.
    public class DieTask extends Task {

        public boolean doTask() {
            //Log.d("Multithread"," Run DieTask");
            return false;
        }
    }

    private class SimpleTaskConsumer implements Runnable {

        @Override
        public void run() {
            int threadId = mNumThreadsAlive++;
            //Log.d("Thread"+threadId, " is ready!");
            do {
                try {
                    final Task task = mTasks.take();
                    if(!task.doTask()){
                        break;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } while (true);

            //Log.d("Multithread",threadId + " quit the loop!");
            mNumThreadsAlive--;


        }
    }
}
