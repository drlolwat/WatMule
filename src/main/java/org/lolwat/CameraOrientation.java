package org.lolwat;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.wrappers.interactive.GameObject;

import java.util.Arrays;
import java.util.Random;

// CREDIT: Neffarion (Dreambot)
public enum CameraOrientation {
    NORTH(2, 0),
    SOUTH(8, 1000),
    EAST(4, 1500),
    WEST(1, 500);

    private final int orientation;
    private final int yaw;

    CameraOrientation(int orientation, int yaw) {
        this.orientation = orientation;
        this.yaw = yaw;
    }

    public static CameraOrientation fromGameObject(GameObject o) {
        return Arrays.stream(values()).filter(i -> i.getOrientation() == o.getOrientation()).findFirst().orElse(null);
    }

    public static CameraOrientation getCurrent() {
        int yaw = Camera.getYaw();

        if (yaw < 1650 && yaw >= 1250) {
            return EAST;
        }

        if (yaw < 1250 && yaw > 760) {
            return SOUTH;
        }

        if (yaw < 350 || yaw >= 1650) {
            return NORTH;
        }

        return WEST;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getYaw() {
        return yaw;
    }

    public CameraOrientation getOpposite() {
        if (this == NORTH) {
            return SOUTH;
        }

        if (this == SOUTH) {
            return NORTH;
        }

        if (this == EAST) {
            return WEST;
        }

        return EAST;
    }

    public void rotateCamera() {
        int yaw = getYaw();
        int rng = (Calculations.random(0, 200) * (new Random().nextBoolean() ? 1 : -1));
        if (this != NORTH) {
            yaw += rng;
        } else {
            yaw += Math.abs(rng);
        }


        Camera.mouseRotateTo(yaw, Camera.getPitch());
    }

}