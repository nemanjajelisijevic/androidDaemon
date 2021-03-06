package com.daemonize.graphics2d.renderer;

import com.daemonize.daemonengine.consumer.DaemonConsumer;
import com.daemonize.daemonengine.consumer.BoundedBufferQueue;

public class DrawConsumer extends DaemonConsumer {

    private Renderer2D renderer;

    public DrawConsumer(Renderer2D renderer, String name, int closureQueueSize) {
        super(name, new BoundedBufferQueue<Runnable>(closureQueueSize));
        this.renderer = renderer;
    }

    @Override
    public boolean consume(Runnable runnable) {
        boolean ret = super.consume(runnable);
        this.renderer.setDirty();
        return ret;
    }
}
