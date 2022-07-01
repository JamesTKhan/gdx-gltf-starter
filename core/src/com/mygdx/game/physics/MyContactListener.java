package com.mygdx.game.physics;

import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;

/**
 * @author James Pooley
 * @version July 01, 2022
 */
public class MyContactListener extends ContactListener {

    @Override
    public boolean onContactAdded(btCollisionObject colObj0, int partId0, int index0, boolean match0, btCollisionObject colObj1, int partId1, int index1, boolean match1) {
        System.out.println("contact");
        return true;
    }
}
