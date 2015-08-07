package com.seizonsenryaku.hayailauncher;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Edgar on 06-Aug-15.
 */
public class SimpleTaskConsumer implements Runnable {
    public static abstract class Task {
        public abstract boolean doTask();
    }
    public static class DieTask extends Task {
        public boolean doTask(){
            return true;
        }
    }

    private final LinkedBlockingQueue<Task> tasks;

    public SimpleTaskConsumer(final PackageManager pm, final Context context,
                              final Activity activity) {
        tasks = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        boolean shouldDie = false;
        do {
            try {
                final Task task = tasks.take();
                shouldDie = task.doTask();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while (!shouldDie);
    }

    public synchronized void addTask(final Task task) {
        try {
            tasks.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public synchronized void removeAllTasks(){
        tasks.clear();
    }
    public synchronized void destroy(){
        removeAllTasks();
        tasks.add(new DieTask());
    }
}
