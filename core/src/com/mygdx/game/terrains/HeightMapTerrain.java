package com.mygdx.game.terrains;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.mygdx.game.terrains.attributes.TerrainFloatAttribute;
import com.mygdx.game.terrains.attributes.TerrainMaterialAttribute;
import com.mygdx.game.terrains.attributes.TerrainTextureAttribute;

/**
 * @author JamesTKhan
 * @version August 07, 2022
 */
public class HeightMapTerrain extends Terrain {

    private final HeightField field;

    public HeightMapTerrain(Pixmap data, float magnitude) {
        this.size = 400;
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

        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("terrain", field.mesh, GL20.GL_TRIANGLES, new Material());
        modelInstance = new ModelInstance(mb.end());

        // Setting the material attributes before model creation was resulting in strange issues
        Material material = modelInstance.materials.get(0);

        TerrainTextureAttribute baseAttribute = TerrainTextureAttribute.createDiffuseBase(getMipMapTexture("textures/Vol_19_4_Base_Color.png"));
        TerrainTextureAttribute terrainSlopeTexture = TerrainTextureAttribute.createDiffuseSlope(getMipMapTexture("textures/Vol_27_4_Base_Color.png"));
        TerrainTextureAttribute terrainHeightTexture = TerrainTextureAttribute.createDiffuseHeight(getMipMapTexture("textures/Vol_16_2_Base_Color.png"));

        baseAttribute.scaleU = 40f;
        baseAttribute.scaleV = 40f;

        TerrainFloatAttribute slope = TerrainFloatAttribute.createMinSlope(0.85f);

        TerrainMaterial terrainMaterial = new TerrainMaterial();
        terrainMaterial.set(baseAttribute);
        terrainMaterial.set(terrainSlopeTexture);
        terrainMaterial.set(terrainHeightTexture);
        terrainMaterial.set(slope);

        material.set(TerrainMaterialAttribute.createTerrainMaterialAttribute(terrainMaterial));
    }

    private Texture getMipMapTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        return texture;
    }

    @Override
    public void dispose() {
        field.dispose();
    }
}
