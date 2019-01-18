package com.daemonize.daemondevapp.view;


import com.daemonize.daemondevapp.images.Image;

public class Button extends CompositeImageViewImpl implements ClickableImageView {

    private Runnable onClickCallback;

    public Button(String name, int relX, int relY, Image image) {
        super(name, relX, relY, image);
    }

    public Button(String name, float absX, float absY, int z, Image image) {
        super(name, absX, absY, z, image);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Button onClick(Runnable onclick) {
        this.onClickCallback = onclick;
        return this;
    }

    @Override
    public boolean checkCoordinates(float x, float y) {
        if (x >= getStartingX() && x <= getEndX()) {
            if (y >= getStartingY() && y <= getEndY() && isShowing()) {
                onClickCallback.run();//TODO should this be here?????
                return true;
            }
        }
        return false;
    }
}
