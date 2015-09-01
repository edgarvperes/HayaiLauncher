package com.seizonsenryaku.hayailauncher.threading;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Edgar on 06-Aug-15.
 */
public class SimpleTaskConsumerManager {

    private final LinkedBlockingQueue<Task> tasks;
    private volatile int numThreadsAlive;
    private boolean consumersShouldDie;
    private SimpleTaskConsumer[] simpleTaskConsumers;
    public SimpleTaskConsumerManager(int numConsumers) {
        tasks = new LinkedBlockingQueue<>();
        startConsumers(numConsumers);

    }

    private void startConsumers(int numConsumers) {
        simpleTaskConsumers = new SimpleTaskConsumer[numConsumers];
        for (int i = 0; i < numConsumers; i++) {
            simpleTaskConsumers[i] = new SimpleTaskConsumer();
            Thread thread = new Thread(simpleTaskConsumers[i]);
            thread.start();
        }
    }

    public void addTask(final Task task) {
        if (consumersShouldDie) return; //TODO throw exception

        try {
            tasks.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void removeAllTasks() {
        tasks.clear();
    }

    public void destroyAllConsumers(boolean finishCurrentTasks) {
        if (consumersShouldDie) return;

        consumersShouldDie = true;
        if (!finishCurrentTasks) removeAllTasks();
        int threadsToKill = numThreadsAlive;
        DieTask dieTask = new DieTask();
        for (int i = 0; i < threadsToKill; i++) {
            tasks.add(dieTask);
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

        }
    }

    private class SimpleTaskConsumer implements Runnable {
        private int threadId;

        @Override
        public void run() {
            threadId = numThreadsAlive++;

            do {
                try {
                    final Task task = tasks.take();
                    task.doTask();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } while (!consumersShouldDie);
            numThreadsAlive--;

        }
    }
}
