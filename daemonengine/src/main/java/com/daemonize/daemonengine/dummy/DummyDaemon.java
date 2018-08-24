package com.daemonize.daemonengine.dummy;

import com.daemonize.daemonengine.closure.Closure;
import com.daemonize.daemonengine.consumer.Consumer;
import com.daemonize.daemonengine.implementations.basedaemon.BaseDaemonEngine;
import com.daemonize.daemonengine.quests.DummyQuest;
import com.daemonize.daemonengine.quests.Quest;

public class DummyDaemon extends BaseDaemonEngine {

    private DummyQuest dummyQuest;

    @Override
    protected Quest getQuest() {
        return dummyQuest;
    }

    public DummyDaemon(Consumer consumer, long sleepInMillis) {
        super(consumer);
        dummyQuest = new DummyQuest().setSleepInterval(sleepInMillis);
    }

    @Override
    public DummyDaemon setConsumer(Consumer consumer) {
        super.setConsumer(consumer);
        return this;
    }

    public DummyDaemon setClosure(Closure<Void> closure) {
        dummyQuest.setClosure(closure);
        return this;
    }

    public DummyDaemon setSleepInterval(int sleepInMillis) {
        dummyQuest.setSleepInterval(sleepInMillis);
        return this;
    }
}