package com.daemonize.game;

import com.daemonize.imagemovers.ImageMover;
import com.daemonize.imagemovers.Movable;

public interface Target<T extends Target> extends Movable, Mortal<T>, Paralyzable<T> {
    boolean isShootable();
    T setShootable(boolean shootable);
}
