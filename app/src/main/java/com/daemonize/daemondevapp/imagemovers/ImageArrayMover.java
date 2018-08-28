package com.daemonize.daemondevapp.imagemovers;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import com.daemonize.daemonengine.utils.DaemonUtils;

public class ImageArrayMover implements ImageMover {

    private volatile Bitmap[] sprite;
    private int cnt = 0;
    protected float initVelocity;

    protected volatile Velocity velocity;
    protected volatile float lastX;
    protected volatile float lastY;

    public synchronized Bitmap[] getSprite() {
        return sprite;
    }

    public synchronized ImageArrayMover setSprite(Bitmap[] sprite) {
        this.sprite = sprite;
        return this;
    }

    @Override
    public Pair<Float, Float> getLastCoordinates() {
        return Pair.create(lastX, lastY);
    }

    @Override
    public Velocity getVelocity() {
        return velocity;
    }

    @Override
    public PositionedBitmap setLastCoordinates(float lastX, float lastY) {
        this.lastX = lastX;
        this.lastY = lastY;

        PositionedBitmap ret = new PositionedBitmap();
        ret.image = iterateSprite();

        ret.positionX = lastX;
        ret.positionY = lastY;

        return ret;
    }

    protected float borderX;
    protected float borderY;

    public ImageArrayMover(Bitmap[] sprite, float velocity, Pair<Float, Float> startingPos) {
        this.sprite = sprite;
        this.initVelocity = velocity;
        this.velocity = new Velocity(velocity, new Direction(80, 20));
        lastX = startingPos.first;
        lastY = startingPos.second;

    }

    protected synchronized Bitmap iterateSprite() {
        Log.w(DaemonUtils.tag(), "CUUUUUUUNT: "+ cnt);
        Bitmap ret = sprite[cnt];
        if (++cnt == sprite.length) {
            cnt = 0;
        }
        return ret;
    }

    @Override
    public void setDirection(Direction direction) {
        this.velocity.direction = direction;
    }

    @Override
    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }

    @Override
    public void setDirectionAndMove(float x, float y, float velocityInt) {
        float dX = x - lastX;
        float dY = y - lastY;
        velocity.intensity = velocityInt;
        velocity.direction = new ImageMover.Direction(dX, dY); //TODO check this shit
    }

    @Override
    @SuppressWarnings("unchecked")
    public ImageArrayMover setBorders(float x, float y) {
        this.borderX = x;
        this.borderY = y;
        return this;
    }

    @Override
    public void setVelocity(float velocity) {
        this.velocity.intensity = velocity;
    }

    @Override
    public void checkCollisionAndBounce(
            Pair<Float, Float> colliderCoordinates,
            Velocity velocity
    ) {}

    @Override
    public PositionedBitmap animate() {

        PositionedBitmap ret = new PositionedBitmap();
        ret.image = iterateSprite();

        //check borders and recalculate
        if (lastX <= 0) {
            lastX = 0;
        } else if (lastX >= borderX) {
            lastX = borderX;
        }

        if(lastY <= 0) {
            lastY = 0;
        } else if( lastY >= borderY) {
            lastY = borderY;
        }

        lastX = lastX +  velocity.intensity * (velocity.direction.coeficientX * 0.01f);
        lastY = lastY + velocity.intensity * (velocity.direction.coeficientY * 0.01f);

        ret.positionX = lastX - ret.image.getWidth()/2;
        ret.positionY = lastY - ret.image.getWidth()/2;

        return ret;
    }


}
