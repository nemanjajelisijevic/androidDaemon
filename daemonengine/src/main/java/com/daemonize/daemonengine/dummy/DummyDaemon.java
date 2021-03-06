package com.daemonize.daemonengine.dummy;

import com.daemonize.daemonengine.DaemonState;
import com.daemonize.daemonengine.Pausable;
import com.daemonize.daemonengine.consumer.Consumer;
import com.daemonize.daemonengine.implementations.BaseDaemonEngine;
import com.daemonize.daemonengine.quests.DummyQuest;
import com.daemonize.daemonengine.quests.BaseQuest;
import com.daemonize.daemonengine.quests.DynamicIntervalDummyQuest;
import com.daemonize.daemonengine.utils.DaemonUtils;

public class DummyDaemon extends BaseDaemonEngine<DummyDaemon> implements Pausable {

    private final DummyQuest dummyQuest;

    public static DummyDaemon create(Consumer consumer, long sleep) {
        return new DummyDaemon(consumer, sleep);
    }

    public static DummyDaemon create(Consumer consumer, DaemonUtils.IntervalRegulator intervalRegulator) {
        return new DummyDaemon(consumer, intervalRegulator);
    }

    @Override
    protected BaseQuest getQuest() {
        return dummyQuest;
    }

    public DummyDaemon(Consumer consumer, long sleepInMillis) {
        super(consumer);
        dummyQuest = new DummyQuest().setConsumer(consumer).setSleepInterval(sleepInMillis);
    }

    public DummyDaemon(Consumer consumer, DaemonUtils.IntervalRegulator regulator) {
        super(consumer);
        dummyQuest = new DynamicIntervalDummyQuest(regulator).setConsumer(consumer);
    }

    @Override
    public DummyDaemon setConsumer(Consumer consumer) {
        super.setConsumer(consumer);
        return this;
    }

    public DummyDaemon setClosure(Runnable closure) {
        dummyQuest.setClosure(closure);
        return this;
    }

    public DummyDaemon setSleepInterval(long sleepInMillis) {
        dummyQuest.setSleepInterval(sleepInMillis);
        return this;
    }

    @Override
    protected boolean runQuest(BaseQuest quest) {
        if(!quest.run()) {
            setDaemonState(DaemonState.GONE_DAEMON);
            return false;
        }
        return true;
    }

    @Override
    public void pause() {
        dummyQuest.pause();
    }

    @Override
    public void cont() {
        dummyQuest.cont();
    }

    @Override
    public DummyDaemon clear() {
        return this;
    }

    public long getCurrentSleepInterval() {
        return dummyQuest.getSleepInterval();
    }
}
