
package com.mygdx.game.terrains;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Disposable;

/**
 * @author JamesTKhan
 * @version August 07, 2022
 */
public abstract class Terrain implements Disposable {
    /** A value we can set to change the actual size of the terrain **/
    protected int size;
    /** Represents the width in vertices, for heightmaps this would be the height map image width **/
    protected int width;
    /** A height scaling factor since heightmap values and noise values are generally between 0.0 - 1.0 **/
    protected float heightMagnitude;

    protected ModelInstance modelInstance;

    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}
