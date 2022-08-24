package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;
import jdk.nashorn.internal.objects.Global;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

public class MyGdxGame extends ApplicationAdapter
{
	private SceneManager sceneManager;
	private PerspectiveCamera camera;
	private Cubemap diffuseCubemap;
	private Cubemap environmentCubemap;
	private Cubemap specularCubemap;
	private Texture brdfLUT;
	private float time;
	private SceneSkybox skybox;
	private DirectionalLightEx light;
	private FirstPersonCameraController cameraController;

	private ParticleSystem particleSystem;
	private ParticleEffect particleEffect;
	private static PFXPool pool;

	@Override
	public void create() {

		// create scene
		sceneManager = new SceneManager();

		// setup camera (The BoomBox model is very small so you may need to adapt camera settings for your scene)
		camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		float d = .02f;
		camera.near = d / 1000f;
		camera.far = 200;
		sceneManager.setCamera(camera);
		camera.position.set(0,0.5f, 20f);

		cameraController = new FirstPersonCameraController(camera);
		Gdx.input.setInputProcessor(cameraController);

		// setup light
		light = new DirectionalLightEx();
		light.direction.set(1, -3, 1).nor();
		light.color.set(Color.WHITE);
		sceneManager.environment.add(light);

		// setup quick IBL (image based lighting)
		IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
		environmentCubemap = iblBuilder.buildEnvMap(1024);
		diffuseCubemap = iblBuilder.buildIrradianceMap(256);
		specularCubemap = iblBuilder.buildRadianceMap(10);
		iblBuilder.dispose();

		// This texture is provided by the library, no need to have it in your assets.
		brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

		sceneManager.setAmbientLight(1f);
		sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
		sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
		sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

		// setup skybox
		skybox = new SceneSkybox(environmentCubemap);
		sceneManager.setSkyBox(skybox);

		// Particle System init
		particleSystem = new ParticleSystem();
		// Since our particle effects are PointSprites, we create a PointSpriteParticleBatch
		BillboardParticleBatch billboardParticleBatch = new BillboardParticleBatch();
		billboardParticleBatch.setCamera(camera);
		particleSystem.add(billboardParticleBatch);

		AssetManager assets = new AssetManager();
		ParticleEffectLoader.ParticleEffectLoadParameter loadParam = new ParticleEffectLoader.ParticleEffectLoadParameter(particleSystem.getBatches());
		assets.load("particles/laser_smoke", ParticleEffect.class, loadParam);
		assets.finishLoading();
		ParticleEffect originalEffect = assets.get("particles/laser_smoke");
		pool = new PFXPool(originalEffect);

		particleEffect = pool.newObject();
		particleEffect.init();
		particleSystem.add(particleEffect);
		particleEffect.start();
	}

	@Override
	public void resize(int width, int height) {
		sceneManager.updateViewport(width, height);
	}

	@Override
	public void render() {
		float deltaTime = Gdx.graphics.getDeltaTime();
		time += deltaTime;

		cameraController.update();

		// render
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		sceneManager.update(deltaTime);
		sceneManager.render();

		particleSystem.update(); // technically not necessary for rendering
		particleSystem.begin();
		particleSystem.draw();
		particleSystem.end();
		sceneManager.getBatch().begin(camera);
		sceneManager.getBatch().render(particleSystem);
		sceneManager.getBatch().end();

		if (particleEffect.isComplete()) {
			particleEffect.start();
		}
	}

	@Override
	public void dispose() {
		sceneManager.dispose();
		environmentCubemap.dispose();
		diffuseCubemap.dispose();
		specularCubemap.dispose();
		brdfLUT.dispose();
		skybox.dispose();
	}

	private static class PFXPool extends Pool<ParticleEffect> {
		private final ParticleEffect sourceEffect;

		public PFXPool(ParticleEffect sourceEffect) {
			this.sourceEffect = sourceEffect;
		}

		@Override
		public void free(ParticleEffect pfx) {
			pfx.reset();
			super.free(pfx);
		}

		@Override
		protected ParticleEffect newObject() {
			return sourceEffect.copy();
		}
	}

}
