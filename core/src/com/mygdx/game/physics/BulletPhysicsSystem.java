package com.mygdx.game.physics;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;

/**
 * @author James Pooley
 * @version July 01, 2022
 */
public class BulletPhysicsSystem implements PhysicsSystem {
    private static final Vector3 localInertia = new Vector3();
    private static final BoundingBox boundingBox = new BoundingBox();

    private final btDynamicsWorld dynamicsWorld;
    private final btCollisionConfiguration collisionConfig;
    private final btDispatcher dispatcher;
    private final btBroadphaseInterface broadphase;
    private final btConstraintSolver constraintSolver;

    private float timeStep = 1/60f;

    private DebugDrawer debugDrawer;

    public BulletPhysicsSystem() {
        // Init Bullet classes
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        constraintSolver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -8f, 0));

        btGImpactCollisionAlgorithm.registerAlgorithm((btCollisionDispatcher) dispatcher);

        debugDrawer = new DebugDrawer();
        debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);
        dynamicsWorld.setDebugDrawer(debugDrawer);

        collisionConfig.obtain();
        dispatcher.obtain();
        broadphase.obtain();
        constraintSolver.obtain();
        dynamicsWorld.obtain();
    }

    @Override
    public void setGravity(float x, float y, float z) {
        dynamicsWorld.setGravity(new Vector3(x,y,z));
    }

    @Override
    public void setTimeStep(float timeStep) {
        this.timeStep = timeStep;
    }

    @Override
    public void update(float delta) {
        // Update physics sim
        dynamicsWorld.stepSimulation(timeStep, 1, 1f / 60f);
    }

    public void drawDebug(Camera camera) {
        debugDrawer.begin(camera);
        dynamicsWorld.debugDrawWorld();
        debugDrawer.end();
    }

    @Override
    public void dispose() {
        collisionConfig.release();
        dispatcher.release();
        broadphase.release();
        constraintSolver.release();
        dynamicsWorld.release();
    }

    public btRigidBody addGimpactBody(ModelInstance modelInstance, float mass) {
        btTriangleIndexVertexArray chassisVertexArray = new btTriangleIndexVertexArray(modelInstance.model.meshParts);
        btGImpactMeshShape shape = new btGImpactMeshShape(chassisVertexArray);
        shape.calculateLocalInertia(mass, localInertia);
        ((btGImpactMeshShape)shape).updateBound();

        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(mass, null, shape, localInertia);
        btRigidBody body = new btRigidBody(info);

        body.setActivationState(Collision.DISABLE_DEACTIVATION);

        MotionState motionState = new MotionState();
        motionState.transform = modelInstance.transform;

        body.setMotionState(motionState);
        body.setCollisionFlags(body.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
        dynamicsWorld.addRigidBody(body);
        return body;
    }

    public btRigidBody addBoxBody(ModelInstance modelInstance, float mass) {
        modelInstance.calculateBoundingBox(boundingBox);

        Vector3 dim = new Vector3();
        boundingBox.getDimensions(dim);
        dim.scl(0.5f);

        btBoxShape shape = new btBoxShape(dim);
        shape.calculateLocalInertia(mass, localInertia);

        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(mass, null, shape, localInertia);
        btRigidBody body = new btRigidBody(info);

        MotionState motionState = new MotionState();
        motionState.transform = modelInstance.transform;

        body.setMotionState(motionState);
        dynamicsWorld.addRigidBody(body);
        return body;
    }

}
