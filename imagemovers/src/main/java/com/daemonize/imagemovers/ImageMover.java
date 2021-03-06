package com.daemonize.imagemovers;


import com.daemonize.daemonengine.utils.Pair;
import com.daemonize.graphics2d.images.Image;
import com.daemonize.daemonprocessor.annotations.SideQuest;


public interface ImageMover extends Movable {

    class Velocity {
        public volatile float intensity;
        public volatile Direction direction;

        public Velocity(float intensity, Direction direction) {
            this.intensity = intensity;
            this.direction = direction;
        }

        //copy construct
        public Velocity(Velocity other) {
            this.intensity = other.intensity;
            this.direction = new Direction(other.direction.coeficientX, other.direction.coeficientY);
        }
    }

    class PositionedImage {
        public Image image;
        public float positionX;
        public float positionY;

        @Override
        public PositionedImage clone() {
            PositionedImage clone = new PositionedImage();
            clone.image = this.image;
            clone.positionX = this.positionX;
            clone.positionY = this.positionY;
            return clone;
        }
    }

    class Direction {

        public volatile float coeficientX;
        public volatile float coeficientY;

        public Direction(float coeficientX, float coeficientY) {
            this.coeficientX = coeficientX;
            this.coeficientY = coeficientY;
        }
    }

    @Override
    Pair<Float, Float> getLastCoordinates();

    Velocity getVelocity();

    void setCoordinates(float lastX, float lastY);

    void setDirection(Direction direction);

    void setVelocity(Velocity velocity);

    boolean setDirectionToPoint(float x, float y);

    boolean setDirectionAndMove(float x, float y, float velocityInt);

    void setVelocity(float velocity);

    <I extends ImageMover> I setBorders(float x1, float x2, float y1, float y2);

    @SideQuest(SLEEP = 25)
    PositionedImage animate() throws InterruptedException;

}
