package com.daemonize.daemonengine.quests;

import com.daemonize.daemonengine.closure.Closure;
import com.daemonize.daemonengine.closure.Return;
import com.daemonize.daemonengine.closure.ReturnRunnable;
import com.daemonize.daemonengine.consumer.Consumer;
import com.daemonize.daemonengine.utils.DaemonUtils;

public abstract class SleepSideQuest<T> extends SideQuest<T> {

    private long sleepInterval;

    @SuppressWarnings("unchecked")
    public <K extends SleepSideQuest<T>> K setSleepInterval(long milliseconds) {
        this.sleepInterval = milliseconds;
        return (K) this;
    }

    @Override
    public final void run(){
        try {

            T result = pursue();
            if (!Thread.currentThread().isInterrupted() && result != null) {
                setResultAndUpdate(result);
            }

            if (sleepInterval > 0) {
                Thread.sleep(sleepInterval);
            }

        } catch (InterruptedException ex) {
            System.out.println(DaemonUtils.tag() + description + " interrupted.");
        } catch (Exception ex) {
            setErrorAndUpdate(ex);
        }
    }

}
