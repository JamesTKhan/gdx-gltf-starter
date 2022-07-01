package com.mygdx.game.physics;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

public class MotionState extends btMotionState {
    public Matrix4 transform;
    public float modelHeightHalved = 0f;

    @Override
    public void getWorldTransform (Matrix4 worldTrans) {
        worldTrans.set(transform);
    }

    @Override
    public void setWorldTransform (Matrix4 worldTrans) {
        transform.set(worldTrans);
        // Offset the transform, half the model height
        transform.translate(0,-modelHeightHalved,0);
    }
}
