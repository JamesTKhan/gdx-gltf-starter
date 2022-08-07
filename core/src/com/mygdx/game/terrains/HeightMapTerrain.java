package com.mygdx.game.terrains;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

/**
 * @author JamesTKhan
 * @version August 07, 2022
 */
public class HeightMapTerrain extends Terrain {

    private final HeightField field;

    public HeightMapTerrain(Pixmap data, float magnitude) {
        this.size = 800;
        this.width = data.getWidth();
        this.heightMagnitude = magnitude;

        field = new HeightField(true, data, true, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        data.dispose();
        field.corner00.set(0, 0, 0);
        field.corner10.set(size, 0, 0);
        field.corner01.set(0, 0, size);
        field.corner11.set(size, 0, size);
        field.magnitude.set(0f, magnitude, 0f);
        field.update();

        Texture texture = new Texture(Gdx.files.internal("textures/sand-dunes1_albedo.png"), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.MipMapLinearLinear);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        PBRTextureAttribute textureAttribute = PBRTextureAttribute.createBaseColorTexture(texture);
        textureAttribute.scaleU = 40f;
        textureAttribute.scaleV = 40f;

        Material material = new Material();
        material.set(textureAttribute);

        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("terrain", field.mesh, GL20.GL_TRIANGLES, material);
        modelInstance = new ModelInstance(mb.end());
    }

    @Override
    public void dispose() {
        field.dispose();
    }
}
