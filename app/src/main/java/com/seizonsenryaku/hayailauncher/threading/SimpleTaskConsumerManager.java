package com.seizonsenryaku.hayailauncher.threading;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Edgar on 06-Aug-15.
 */
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
