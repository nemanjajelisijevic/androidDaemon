package com.daemonize.imagemovers;

import com.daemonize.graphics2d.images.Image;

public class AngleToImageArray {

    private Image[] array = new Image[360];
    private int step;
    private int pointer = 0;


    public void setCurrentAngle(int degrees) {

        if (degrees >= 360)
            degrees = degrees - 360;

        if (degrees < 0 || degrees > 359) {
            throw new IllegalArgumentException("Arg degrees must be > 0 && < 360, set: " + degrees);
        }
        pointer = degrees;
    }

    public int getStep() {
        return step;
    }

    public AngleToImageArray(Image[] sprite) {
        this(sprite, 360 / sprite.length);
    }

    public AngleToImageArray(Image[] sprite, int step) {
        this.step = step;
        for (int i = 0; i < array.length; ++i) {
            int it = i / step;
            if (it < sprite.length)
                array[i] = sprite[it];
            else
                break;
        }
    }

    public Image getCurrent() {
        return array[pointer];
    }

    public int getCurrentAngle() {
        return pointer;
    }

    public Image getIncrementedByStep() {
        int diff = pointer + step - array.length;
        if(diff >= 0) {
            pointer = diff;
        } else {
            pointer += step;
        }
        return array[pointer];
    }

    public Image getDecrementedByStep() {
        int diff = pointer - step;
        if (diff < 0) {
            pointer = array.length + diff;
        } else if (diff == 0){
            pointer = 0;
        } else {
            pointer -= step;
        }
        return array[pointer];
    }

    public Image getByAngle(int degrees) {
        return array[degrees];
    }


}
