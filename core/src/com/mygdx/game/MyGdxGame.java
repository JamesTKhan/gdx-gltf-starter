package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.enums.CameraMode;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

public class MyGdxGame extends ApplicationAdapter implements AnimationController.AnimationListener, InputProcessor
{
	private SceneManager sceneManager;
	private SceneAsset sceneAsset;
	private Scene playerScene;
	private PerspectiveCamera camera;
	private Cubemap diffuseCubemap;
	private Cubemap environmentCubemap;
	private Cubemap specularCubemap;
	private Texture brdfLUT;
	private float time;
	private SceneSkybox skybox;
	private DirectionalLightEx light;

	// Player Movement
	float speed = 5f;
	float rotationSpeed = 80f;
	private final Matrix4 playerTransform = new Matrix4();
	private final Vector3 moveTranslation = new Vector3();
	private final Vector3 currentPosition = new Vector3();

	// Camera
	private CameraMode cameraMode = CameraMode.BEHIND_PLAYER;
	private float camPitch = Settings.CAMERA_START_PITCH;
	private float distanceFromPlayer = 35f;
	private float angleAroundPlayer = 0f;
	private float angleBehindPlayer = 0f;

	@Override
	public void create() {

		// create scene
		sceneAsset = new GLTFLoader().load(Gdx.files.internal("models/Alien Slime.gltf"));
		playerScene = new Scene(sceneAsset.scene);
		sceneManager = new SceneManager();
		sceneManager.addScene(playerScene);

		camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.near = 1f;
		camera.far = 200;
		sceneManager.setCamera(camera);
		camera.position.set(0,0, 4f);

		Gdx.input.setCursorCatched(true);
		Gdx.input.setInputProcessor(this);

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

		playerScene.animationController.setAnimation("idle", -1);
		buildBoxes();
	}

	@Override
	public void resize(int width, int height) {
		sceneManager.updateViewport(width, height);
	}

	@Override
	public void render() {
		float deltaTime = Gdx.graphics.getDeltaTime();
		time += deltaTime;

		processInput(deltaTime);
		updateCamera();

		if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
			playerScene.animationController.action("jump", 1, 1f, this, 0.5f);

		// render
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		sceneManager.update(deltaTime);
		sceneManager.render();
	}

	private void processInput(float deltaTime) {
		// Update the player transform
		playerTransform.set(playerScene.modelInstance.transform);

		if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
			Gdx.app.exit();
		}

		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			moveTranslation.z += speed * deltaTime;
		}

		if (Gdx.input.isKeyPressed(Input.Keys.S)) {
			moveTranslation.z -= speed * deltaTime;
		}

		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			playerTransform.rotate(Vector3.Y, rotationSpeed * deltaTime);
			angleBehindPlayer += rotationSpeed * deltaTime;
		}

		if (Gdx.input.isKeyPressed(Input.Keys.D)) {
			playerTransform.rotate(Vector3.Y, -rotationSpeed * deltaTime);
			angleBehindPlayer -= rotationSpeed * deltaTime;
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
			switch (cameraMode) {

				case FREE_LOOK:
					cameraMode = CameraMode.BEHIND_PLAYER;
					angleAroundPlayer = angleBehindPlayer;
					break;
				case BEHIND_PLAYER:
					cameraMode = CameraMode.FREE_LOOK;
					break;
			}
		}

		// Apply the move translation to the transform
		playerTransform.translate(moveTranslation);

		// Set the modified transform
		playerScene.modelInstance.transform.set(playerTransform);

		// Update vector position
		playerScene.modelInstance.transform.getTranslation(currentPosition);

		// Clear the move translation out
		moveTranslation.set(0,0,0);
	}

	private void buildBoxes() {
		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();

		for (int x = 0; x < 100; x+= 10) {
			for (int z = 0; z < 100; z+= 10) {
				Material material = new Material();
				material.set(PBRColorAttribute.createBaseColorFactor(Color.RED));
				MeshPartBuilder builder = modelBuilder.part(x + ", " + z, GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, material);
				BoxShapeBuilder.build(builder, x, 0, z, 1f,1f,1f);
			}
		}

		ModelInstance model = new ModelInstance(modelBuilder.end());
		sceneManager.addScene(new Scene(model));
	}

	private void updateCamera() {
		float horDistance = calculateHorizontalDistance(distanceFromPlayer);
		float vertDistance = calculateVerticalDistance(distanceFromPlayer);

		calculatePitch();
		calculateAngleAroundPlayer();
		calculateCameraPosition(currentPosition, horDistance, vertDistance);

		camera.up.set(Vector3.Y);
		camera.lookAt(currentPosition);
		camera.update();
	}

	private void calculateCameraPosition(Vector3 currentPosition, float horDistance, float vertDistance) {
		float offsetX = (float) (horDistance * Math.sin(Math.toRadians(angleAroundPlayer)));
		float offsetZ = (float) (horDistance * Math.cos(Math.toRadians(angleAroundPlayer)));

		camera.position.x = currentPosition.x - offsetX;
		camera.position.z = currentPosition.z - offsetZ;
		camera.position.y = currentPosition.y + vertDistance;
	}

	private void calculateAngleAroundPlayer() {
		if (cameraMode == CameraMode.FREE_LOOK) {
			float angleChange = Gdx.input.getDeltaX() * Settings.CAMERA_ANGLE_AROUND_PLAYER_FACTOR;
			angleAroundPlayer -= angleChange;
		} else {
			angleAroundPlayer = angleBehindPlayer;
		}
	}

	private void calculatePitch() {
		float pitchChange = -Gdx.input.getDeltaY() * Settings.CAMERA_PITCH_FACTOR;
		camPitch -= pitchChange;

		if (camPitch < Settings.CAMERA_MIN_PITCH)
			camPitch = Settings.CAMERA_MIN_PITCH;
		else if (camPitch > Settings.CAMERA_MAX_PITCH)
			camPitch = Settings.CAMERA_MAX_PITCH;
	}

	private float calculateVerticalDistance(float distanceFromPlayer) {
		return (float) (distanceFromPlayer * Math.sin(Math.toRadians(camPitch)));
	}

	private float calculateHorizontalDistance(float distanceFromPlayer) {
		return (float) (distanceFromPlayer * Math.cos(Math.toRadians(camPitch)));
	}

	@Override
	public void dispose() {
		sceneManager.dispose();
		sceneAsset.dispose();
		environmentCubemap.dispose();
		diffuseCubemap.dispose();
		specularCubemap.dispose();
		brdfLUT.dispose();
		skybox.dispose();
	}

	@Override
	public void onEnd(AnimationController.AnimationDesc animation) {

	}

	@Override
	public void onLoop(AnimationController.AnimationDesc animation) {

	}

	@Override
	public boolean keyDown(int keycode) {
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		float zoomLevel = amountY * Settings.CAMERA_ZOOM_LEVEL_FACTOR;
		distanceFromPlayer += zoomLevel;
		if (distanceFromPlayer < Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER)
			distanceFromPlayer = Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER;
		return false;
	}
}
