package com.daemonize.daemonengine.consumer;


import com.daemonize.daemonengine.Daemon;
import com.daemonize.daemonengine.DaemonState;
import com.daemonize.daemonengine.utils.DaemonUtils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DaemonConsumer implements Consumer, Daemon {

    private volatile DaemonState state = DaemonState.STOPPED;
    private Queue<Runnable> closureQueue = new LinkedList<>();
    private final Lock closureLock = new ReentrantLock();
    private Condition closureAvailable = closureLock.newCondition();
    private String name;
    private Thread looperThread;

    public DaemonConsumer(String name) {
        this.name = name;
    }

    @Override
    public boolean enqueue(Runnable runnable) {
        boolean ret;
        closureLock.lock();
        ret = closureQueue.add(runnable);
        closureAvailable.signal();
        closureLock.unlock();
        return ret;
    }

    private Runnable dequeue() {
        Runnable ret = null;
        try {
            closureLock.lock();
            while (closureQueue.isEmpty()) {
                state = DaemonState.IDLE;
                closureAvailable.await();
            }
            ret = closureQueue.poll();
        } catch (InterruptedException ex) {
            System.out.println(DaemonUtils.tag() + " Waiting on a closure interrupted");
        } finally {
            closureLock.unlock();
        }
        return ret;
    }

    private void loop() {
        while (!state.equals(DaemonState.STOPPED)) {
            Runnable currentRunnable = dequeue();
            if (currentRunnable == null) {
                break;
            }
            dequeue().run();
        }
    }

    @Override
    public void start() {
        DaemonState initState = getState();
        if (!(initState.equals(DaemonState.STOPPED))) {
            System.out.println(DaemonUtils.tag() +  name + " already running. State: " + getState());
        } else {
            looperThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    loop();
                }
            });
            looperThread.setName(name);
            state = DaemonState.INITIALIZING;
            looperThread.start();
        }
    }

    @Override
    public void stop() {
        state = DaemonState.STOPPED;
        if (looperThread != null && !Thread.currentThread().equals(looperThread) && looperThread.isAlive()) {
            looperThread.interrupt();
        }
    }

    @Override
    public DaemonState getState() {
        return state;
    }

    @Override
    public <K extends Daemon> K setName(String name) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <K extends Daemon> K setConsumer(Consumer consumer) {
        throw new IllegalStateException("This object already encapsulates a consumer thread. This operation is not permitted!");
    }
}
