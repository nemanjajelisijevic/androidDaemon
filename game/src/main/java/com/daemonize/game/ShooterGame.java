package com.daemonize.game;

import com.daemonize.daemonengine.closure.Closure;
import com.daemonize.daemonengine.closure.Return;
import com.daemonize.daemonengine.consumer.DaemonConsumer;
import com.daemonize.daemonengine.daemonscript.DaemonChainScript;
import com.daemonize.daemonengine.utils.DaemonUtils;
import com.daemonize.daemonengine.utils.Pair;
import com.daemonize.game.controller.MovementController;
import com.daemonize.game.controller.MovementControllerDaemon;
import com.daemonize.game.grid.Field;
import com.daemonize.game.grid.Grid;
import com.daemonize.game.interactables.Interactable;
import com.daemonize.game.interactables.health.HealthPack;
import com.daemonize.graphics2d.camera.Camera2D;
import com.daemonize.graphics2d.images.Image;
import com.daemonize.graphics2d.images.imageloader.ImageManager;
import com.daemonize.graphics2d.renderer.Renderer2D;
import com.daemonize.graphics2d.scene.Scene2D;
import com.daemonize.graphics2d.scene.views.FixedView;
import com.daemonize.graphics2d.scene.views.ImageView;
import com.daemonize.graphics2d.scene.views.ImageViewImpl;
import com.daemonize.imagemovers.ImageMover;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import daemon.com.commandparser.CommandParser;
import daemon.com.commandparser.CommandParserDaemon;

public class ShooterGame {

    //animate closure def
    private static class PlayerCameraClosure implements Closure<ImageMover.PositionedImage[]> {

        private ImageView mainView, hpView, searchlight;

        public PlayerCameraClosure(ImageView mainView, ImageView hpView, ImageView searchlight) {
            this.mainView = mainView;
            this.hpView = hpView;
            this.searchlight = searchlight;
        }

        @Override
        public void onReturn(Return<ImageMover.PositionedImage[]> ret) {
            ImageMover.PositionedImage[] result = ret.runtimeCheckAndGet();
            mainView.setAbsoluteX(result[0].positionX)
                    .setAbsoluteY(result[0].positionY)
                    .setImage(result[0].image);
            hpView.setAbsoluteX(result[1].positionX)
                    .setAbsoluteY(result[1].positionY)
                    .setImage(result[1].image);
            searchlight.setAbsoluteX(result[2].positionX)
                    .setAbsoluteY(result[2].positionY)
                    .setImage(result[2].image);
        }
    }

    //running flag
    private volatile boolean running;

    //pause flag
    private volatile boolean paused;

    //game consumer threads
    private Renderer2D renderer;
    private DaemonConsumer gameConsumer;

    //image loader
    private ImageManager imageManager;

    //daemonState holder
    private DaemonChainScript stateChain = new DaemonChainScript();

    //Scene
    private Scene2D scene;

    //BackgroundImage
    private Image backgroundImage;
    private ImageView backgroundView;

    //map borders
    private Integer borderX;
    private Integer borderY;

    //screen borders
    int cameraWidth, cameraHeight;

    //grid
    private Grid<Interactable<PlayerDaemon>> grid;
    private int rows;
    private int columns;
    //private ImageView[][] gridViewMatrix;

    private int fieldWidth;

    private Image accessibleField;
    private Image inaccessibleField;


    //camera
    private Camera2D camera;

    //cmd parser
    private CommandParserDaemon commandParser;

    //random int
    private Random random = new Random();

    private int getRandomInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    //resolution scaling attribute
    private float dXY;

    //player
    private PlayerDaemon player;

    private Image[] playerSprite;
    private Image[] healthBarSprite;
    private Image searchlight;

    private Image healthPackImage;
    private ImageView healthPackView;

    private List<Field<Interactable<PlayerDaemon>>> healthPackFields;

    //controller
    private MovementControllerDaemon controller;

    public MovementControllerDaemon getController() {
        return controller;
    }

    //construct
    public ShooterGame(Renderer2D renderer, ImageManager imageManager, MovementController controller, int width, int height) {

        this.renderer = renderer;
        this.imageManager = imageManager;

        this.gameConsumer = new DaemonConsumer("Game Consumer");

        this.cameraWidth = width;
        this.cameraHeight = height;

        int screenToMapRatio = 5;
        this.borderX = width * screenToMapRatio;
        this.borderY = height * screenToMapRatio;

        this.camera = new FollowingCamera(width, height);
        this.scene = new Scene2D();
        this.dXY = ((float) cameraWidth) / 1000;

        this.controller = new MovementControllerDaemon(gameConsumer, controller).setName("Player con");

        this.fieldWidth = 50;
        this.rows = borderY / fieldWidth;
        this.columns = borderX / fieldWidth;

        this.grid = new Grid<Interactable<PlayerDaemon>>(
                rows,
                columns,
                Pair.create(0, 0),
                Pair.create(rows - 1, columns - 1),
                0,
                0,
                borderX / columns
        );

        this.healthPackFields = new LinkedList<>();
    }

    public ShooterGame run() {

        gameConsumer.start().consume(() -> {

            this.running = true;
            this.paused = false;
            commandParser = new CommandParserDaemon(new CommandParser(this));
            commandParser.setParseSideQuest();
            //commandParser.start();
            gameConsumer.consume(stateChain::run);
        });

        return this;
    }

    {
        stateChain.addState(()->{ //image loading

            try {

                backgroundImage = imageManager.loadImageFromAssets(
                        "map_1.png",
                        this.borderX,
                        this.borderY
                );

                ImageView backgroundView = new ImageViewImpl("Background View")
                        .setAbsoluteX(borderX / 2)
                        .setAbsoluteY(borderY / 2)
                        .setImage(backgroundImage)
                        .setZindex(0)
                        .show();

                scene.addImageView(backgroundView);

                //init grid views
                accessibleField = imageManager.loadImageFromAssets("greenOctagon.png", fieldWidth, fieldWidth);
                inaccessibleField = imageManager.loadImageFromAssets("redOctagon.png", fieldWidth, fieldWidth);

//                gridViewMatrix = new ImageView[rows][columns];
//
//                for (int j = 0; j < rows; ++j ) {
//                    for (int i = 0; i < columns; ++i)
//                        gridViewMatrix[j][i] = scene.addImageView(new ImageViewImpl("Grid [" + j + "][" + i +"]"))
//                                .setAbsoluteX(grid.getGrid()[j][i].getCenterX())
//                                .setAbsoluteY(grid.getGrid()[j][i].getCenterY())
//                                .setImage(grid.getField(j, i).isWalkable() ? accessibleField : inaccessibleField)
//                                .setZindex(3)
//                                .hide();
//                }

                //init player sprites
                int playerWidth = cameraWidth / 10;
                int playerHeight = cameraHeight / 10;

                playerSprite = new Image[36];

                for (int i = 0; i < 36; i++) {
                    playerSprite[i] = imageManager.loadImageFromAssets(
                            "plane" + i + "0.png",
                            playerWidth,
                            playerHeight
                    );
                }

                int width_hp = (playerWidth * 3) / 4;
                int height_hp = playerHeight / 5;

                healthBarSprite = new Image[10];
                for (int i = 0; i < healthBarSprite.length; ++i) {
                    healthBarSprite[i] = imageManager.loadImageFromAssets(
                            "health_bar_" + (i + 1) + "0.png",
                            width_hp, height_hp
                    );
                }

                searchlight = imageManager.loadImageFromAssets("searchlight.png", playerWidth / 2, playerHeight);
                healthPackImage = imageManager.loadImageFromAssets("healthPack.png", playerWidth / 2, playerWidth /2 );

                healthPackFields.add(grid.getField(21, 25));
                healthPackFields.add(grid.getField(11, 15));
                healthPackFields.add(grid.getField(11, 35));
                healthPackFields.add(grid.getField(2, 15));
                healthPackFields.add(grid.getField(getRandomInt(0, rows), getRandomInt(0, columns)));
                healthPackFields.add(grid.getField(getRandomInt(0, rows), getRandomInt(0, columns)));

                for(Field<Interactable<PlayerDaemon>> current : healthPackFields) {
                    current.setObject(
                            HealthPack.generateHealthPack(
                                    70,
                                    ((int) current.getCenterX()),
                                    ((int) current.getCenterY()),
                                    healthPackImage, scene
                            )
                    );
                }

                //init player
                player = new PlayerDaemon(
                        gameConsumer,
                        new Player(
                                playerSprite,
                                healthBarSprite,
                                searchlight,
                                Pair.create((float)(borderX / 2), (float) (borderY / 2)),
                                dXY,
                                cameraWidth / 2,
                                cameraHeight / 2,
                                100,
                                10
                        )
                ).setName("Player");

                {
                    ImageView mainView = scene.addImageView(new FixedView("Player Main View", cameraWidth / 2, cameraHeight / 2))
                            .setImage(playerSprite[0])
                            .setAbsoluteX(borderX / 2)
                            .setAbsoluteY(borderY / 2)
                            .setZindex(10);

                    ImageView hpView = scene.addImageView(new FixedView("Player HP View", cameraWidth / 2, cameraHeight / 2 - playerSprite[0].getHeight() / 2))
                            .setImage(healthBarSprite[0])
                            .setAbsoluteX(borderX / 2)
                            .setAbsoluteY(borderY / 2)
                            .setZindex(10);

                    ImageView searchlightView = scene.addImageView(new FixedView("Player Searchlight View", cameraWidth / 2, cameraHeight / 2 + playerSprite[0].getHeight() / 2)
                            .setImage(searchlight)
                            .setAbsoluteX(borderX / 2)
                            .setAbsoluteY(borderY / 2)
                            .setZindex(9)
                    );

                    renderer.consume(() -> {
                        mainView.show();
                        hpView.show();
                        searchlightView.show();
                    });

                    player.setAnimatePlayerSideQuest(renderer).setClosure(new PlayerCameraClosure(mainView, hpView, searchlightView));
                }

                renderer.setScene(scene.lockViews()).start();

                ((FollowingCamera) camera).setTarget(player);

                renderer.setCamera(camera);

                controller.getPrototype().setControllable(player.start());

                KeyBoardMovementController controllerPrototype = ((KeyBoardMovementController) controller.getPrototype());

                controllerPrototype.setConsumer(gameConsumer);

                controllerPrototype.setDirMapper(new MovementController.DirectionToCoordinateMapper() {
                    @Override
                    public Pair<Float, Float> map(MovementController.Direction dir) {

                        Field currentField = grid.getField(
                                player.getLastCoordinates().getFirst(),
                                player.getLastCoordinates().getSecond()
                        );

                        List<Field> neighbors = grid.getNeighbors(currentField);

                        Pair<Float, Float> ret = null;

                        switch (dir) {
                            case UP:
                                ret = Pair.create(neighbors.get(1).getCenterX(), neighbors.get(1).getCenterY());
                                break;
                            case DOWN:
                                ret = Pair.create(neighbors.get(6).getCenterX(), neighbors.get(6).getCenterY());
                                break;
                            case RIGHT:
                                ret = Pair.create(neighbors.get(4).getCenterX(), neighbors.get(4).getCenterY());
                                break;
                            case LEFT:
                                ret = Pair.create(neighbors.get(3).getCenterX(), neighbors.get(3).getCenterY());
                                break;
                            case UP_RIGHT:
                                ret = Pair.create(neighbors.get(2).getCenterX(), neighbors.get(2).getCenterY());
                                break;
                            case UP_LEFT:
                                ret = Pair.create(neighbors.get(0).getCenterX(), neighbors.get(0).getCenterY());
                                break;
                            case DOWN_RIGHT:
                                ret = Pair.create(neighbors.get(7).getCenterX(), neighbors.get(7).getCenterY());
                                break;
                            case DOWN_LEFT:
                                ret = Pair.create(neighbors.get(5).getCenterX(), neighbors.get(5).getCenterY());
                                break;
                            default:
                                throw new IllegalStateException("No dir: " + dir);

                        }

                        return ret;
                    }
                });

                controllerPrototype.setMovementCallback(player -> {

                    Field<Interactable<PlayerDaemon>> field = grid.getField(player.getLastCoordinates().getFirst(), player.getLastCoordinates().getSecond());
                    System.out.println(DaemonUtils.tag() + "Actual player coordinates: " + player.getLastCoordinates().toString());
                    System.err.println(DaemonUtils.tag() + "Player at field: " + field.toString());

                    //renderer.consume(gridViewMatrix[field.getRow()][field.getColumn()]::show);

                    Interactable<PlayerDaemon> item = field.getObject();

                    if (item != null) {
                        item.interact(player);
                        renderer.consume(item.getView()::hide);
                        field.setObject(null);
                    }
                });

                controller.setControlSideQuest();
                controller.start();

                Field firstField = grid.getField(rows / 2, columns / 2);

                player.rotateTowards(firstField.getCenterX(), firstField.getCenterY())
                        .go(firstField.getCenterX(), firstField.getCenterY(), 2F);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}