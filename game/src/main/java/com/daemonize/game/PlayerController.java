package com.daemonize.game;

import com.daemonize.daemonengine.utils.DaemonSemaphore;
import com.daemonize.daemonengine.utils.Pair;
import com.daemonize.game.controller.DirectionController;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PlayerController implements DirectionController {

    private PlayerDaemon player;

    private float distanceOffset;
    private float diagonalDistanceOffset;

    private final float controllerVelocity;
    private final float speedUpVelocity;

    private volatile float currentVelocity;

    private LinkedList<Direction> pressedDirections;
    private Lock queueLock;
    private Condition queueEmptyCondition;

    private DaemonSemaphore playerMovementSemaphore;

    //on PlayerDaemons consumer
    private int playerMovementCounter;
    private Runnable playerMovementClosure = () -> {
        if (++playerMovementCounter % 2 == 0)
            playerMovementSemaphore.go();
    };

    public PlayerController(PlayerDaemon player) {
        this.player = player;
        this.pressedDirections = new LinkedList<>();
        this.queueLock = new ReentrantLock();
        this.queueEmptyCondition = queueLock.newCondition();
        this.distanceOffset = 10;
        this.diagonalDistanceOffset = distanceOffset * 0.71F;
        this.controllerVelocity = 4.5F;
        this.speedUpVelocity = controllerVelocity * 3;
        this.currentVelocity = controllerVelocity;
        this.playerMovementSemaphore = new DaemonSemaphore();
    }

    @Override
    public void pressDirection(Direction dir) {

        queueLock.lock();

        if (pressedDirections.isEmpty()) {

            pressedDirections.add(dir);
            queueEmptyCondition.signalAll();

        } else {

            if (pressedDirections.size() == 1 && !pressedDirections.get(0).equals(dir))
                pressedDirections.add(dir);
            else if (pressedDirections.size() == 2 && !pressedDirections.contains(dir)) {
                pressedDirections.poll();
                pressedDirections.add(dir);
            }
        }

        queueLock.unlock();
    }

    @Override
    public void releaseDirection(Direction dir) {

        queueLock.lock();

        if(!pressedDirections.isEmpty() && pressedDirections.get(0).equals(dir)) {

            pressedDirections.poll();

            if (!pressedDirections.isEmpty() && pressedDirections.get(0).equals(dir))
                pressedDirections.poll();

        } else if (pressedDirections.size() == 2 && pressedDirections.get(1).equals(dir))
            pressedDirections.remove(1);

        queueLock.unlock();
    }

    @Override
    public void speedUp() {
        currentVelocity = speedUpVelocity;
    }

    @Override
    public void speedDown() {
        currentVelocity = controllerVelocity;
    }

    @Override
    public void control() throws InterruptedException {

        try {

            playerMovementSemaphore.await();

            queueLock.lock();

            while (pressedDirections.isEmpty())
                queueEmptyCondition.await();

            Pair<Float, Float> playerCoord = player.getLastCoordinates();

            if(pressedDirections.size() == 1) {

                DirectionController.Direction dir = pressedDirections.peek();

                switch (dir) {

                    case UP:
                        playerCoord.setSecond(playerCoord.getSecond() - distanceOffset);
                        break;

                    case DOWN:
                        playerCoord.setSecond(playerCoord.getSecond() + distanceOffset);
                        break;

                    case LEFT:
                        playerCoord.setFirst(playerCoord.getFirst() - distanceOffset);
                        break;

                    case RIGHT:
                        playerCoord.setFirst(playerCoord.getFirst() + distanceOffset);
                        break;

                    default:
                        throw new IllegalStateException("Unknown direction" + dir);
                }

                playerMovementSemaphore.stop();
                player.rotateTowards(playerCoord, playerMovementClosure)
                        .goTo(playerCoord, currentVelocity, playerMovementClosure);

            } else if (pressedDirections.size() == 2) {

                DirectionController.Direction dirOne = pressedDirections.get(0);
                DirectionController.Direction dirTwo = pressedDirections.get(1);

                switch (dirOne) {
                    case UP:

                        playerCoord.setSecond(playerCoord.getSecond() - diagonalDistanceOffset);

                        if (dirTwo.equals(Direction.RIGHT))
                            playerCoord.setFirst(playerCoord.getFirst() + diagonalDistanceOffset);
                        else if (dirTwo.equals(Direction.LEFT))
                            playerCoord.setFirst(playerCoord.getFirst() - diagonalDistanceOffset);

                        break;

                    case DOWN:

                        playerCoord.setSecond(playerCoord.getSecond() + diagonalDistanceOffset);

                        if (dirTwo.equals(Direction.RIGHT))
                            playerCoord.setFirst(playerCoord.getFirst() + diagonalDistanceOffset);
                        else if (dirTwo.equals(Direction.LEFT))
                            playerCoord.setFirst(playerCoord.getFirst() - diagonalDistanceOffset);

                        break;

                    case LEFT:
                        playerCoord.setFirst(playerCoord.getFirst() - diagonalDistanceOffset);

                        if (dirTwo.equals(Direction.UP))
                            playerCoord.setSecond(playerCoord.getSecond() - diagonalDistanceOffset);
                        else if (dirTwo.equals(Direction.DOWN))
                            playerCoord.setSecond(playerCoord.getSecond() + diagonalDistanceOffset);

                        break;

                    case RIGHT:
                        playerCoord.setFirst(playerCoord.getFirst() + diagonalDistanceOffset);

                        if (dirTwo.equals(Direction.UP))
                            playerCoord.setSecond(playerCoord.getSecond() - diagonalDistanceOffset);
                        else if (dirTwo.equals(Direction.DOWN))
                            playerCoord.setSecond(playerCoord.getSecond() + diagonalDistanceOffset);

                        break;

                    default:
                        throw new IllegalStateException("Unknown direction" + dirOne + ", dirTwo " + dirTwo);

                }

                playerMovementSemaphore.stop();
                player.rotateTowards(playerCoord, playerMovementClosure)
                        .goTo(playerCoord, currentVelocity, playerMovementClosure);

            }

        } finally {
            queueLock.unlock();
        }
    }
}
