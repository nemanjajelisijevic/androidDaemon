package com.daemonize.game;

import com.daemonize.daemonengine.utils.Pair;
import com.daemonize.daemonprocessor.annotations.Daemon;
import com.daemonize.daemonprocessor.annotations.Daemonize;
import com.daemonize.daemonprocessor.annotations.DedicatedThread;
import com.daemonize.daemonprocessor.annotations.GenerateRunnable;
import com.daemonize.daemonprocessor.annotations.SideQuest;
import com.daemonize.graphics2d.images.Image;
import com.daemonize.graphics2d.scene.views.ImageView;
import com.daemonize.imagemovers.CoordinatedImageTranslationMover;
import com.daemonize.imagemovers.RotatingSpriteImageMover;

import java.beans.IntrospectionException;
import java.util.Arrays;

@Daemon(doubleDaemonize = true, implementPrototypeInterfaces = true)
public class Player extends CoordinatedImageTranslationMover implements Target<Player> {

    private final int screenCenterX, screenCenterY;
    private Image[] spriteHealthBarImage;

    private volatile int hpMax;
    private volatile int hp;
    private volatile boolean shootable = true;

    private PositionedImage[] ret;
    private RotatingSpriteImageMover rotationMover;

    public Player(Image[] sprite, Image[] hpSprite, Pair<Float, Float> startingPos, float dXY, int screenCenterX, int screenCenterY, int hpMax, int hp) {
        super(sprite[0], startingPos, dXY);
        this.spriteHealthBarImage = hpSprite;
        this.screenCenterX = screenCenterX;
        this.screenCenterY = screenCenterY;
        this.rotationMover = new RotatingSpriteImageMover(sprite, animateSemaphore, startingPos, dXY);
        this.ret = new PositionedImage[2];
        this.ret[1] = new PositionedImage();
        this.ret[1].positionX = this.screenCenterX;
        this.ret[1].positionY = this.screenCenterY - (sprite[0].getHeight() / 2);
        this.hp = hp;
        this.hpMax = hpMax;
    }

    @Daemonize
    @DedicatedThread(engineName = "rotate")
    public void rotateTowards(float x, float y) throws InterruptedException {
        rotationMover.rotateTowards(getLastCoordinates().getFirst(), getLastCoordinates().getSecond(), x, y);
    }

    @Daemonize
    @GenerateRunnable
    @DedicatedThread(engineName = "rotate")
    public void rotateTowards(Pair<Float, Float> coords) throws InterruptedException {
        rotateTowards(coords.getFirst(), coords.getSecond());
    }

    @Daemonize
    @Override
    public boolean goTo(float x, float y, float velocity) throws InterruptedException {
        return super.goTo(x, y, velocity);
    }

    @Daemonize
    @GenerateRunnable
    public void goTo(Pair<Float, Float> coords, float velocity) throws InterruptedException {
        super.goTo(coords.getFirst(), coords.getSecond(), velocity);
    }

    @Daemonize
    public void go(float x, float y, float velocity) throws InterruptedException {
        goTo(x, y, velocity);
    }

    @Daemonize
    public void go(Pair<Float, Float> coords, float velocity) throws InterruptedException {
        go(coords.getFirst(), coords.getSecond(), velocity);
    }

    @Daemonize
    public boolean rotAndGo(float x, float y, float velocity) throws InterruptedException {
        rotateTowards(x, y);
        return goTo(x, y, velocity);
    }

    @Daemonize
    public boolean rotAndGo(Pair<Float, Float> coords, float velocity) throws InterruptedException {
        return rotAndGo(coords.getFirst(), coords.getSecond(), velocity);
    }

    @Override
    public boolean isShootable() {
        return shootable;
    }

    @Override
    public Player setShootable(boolean shootable) {
        this.shootable = shootable;
        return this;
    }

    @Override
    public int getHp() {
        return hp;
    }

    @Override
    public Player setHp(int hp) {
        if (hp > hpMax)
            this.hp = hpMax;
        else
            this.hp = hp;
        return this;
    }

    @Override
    public Player setMaxHp(int maxHp) {
        this.hpMax = maxHp;
        return this;
    }

    @Override
    public int getMaxHp() {
        return hpMax;
    }

    @Override
    public boolean isParalyzed() {
        return false;
    }

    @Override
    public Player setParalyzed(boolean paralyzed) {
        return this;
    }

    @Override
    public Image iterateSprite() {
        return rotationMover.iterateSprite();
    }

    @SideQuest(SLEEP = 15, blockingClosure = true)
    public PositionedImage[] animatePlayer() throws InterruptedException {

        this.ret[0] = super.animate();

        if(this.ret[0] == null)
            return null;

        this.ret[0].positionX = screenCenterX;
        this.ret[0].positionY = screenCenterY;
        this.ret[1].image = spriteHealthBarImage[(hp * 100 / hpMax - 1) / spriteHealthBarImage.length];

        return this.ret;
    }
}
