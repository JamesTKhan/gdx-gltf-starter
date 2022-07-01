package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.mygdx.game.enums.CameraMode;
import com.mygdx.game.physics.BulletPhysicsSystem;
import com.mygdx.game.physics.MyContactListener;
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
	float speed = 55f;
	float rotationSpeed = 80f;
	private final Matrix4 playerTransform = new Matrix4();
	private Vector3 moveTranslation = new Vector3();
	private final Vector3 currentPosition = new Vector3();

	// Camera
	private CameraMode cameraMode = CameraMode.BEHIND_PLAYER;
	private float camPitch = Settings.CAMERA_START_PITCH;
	private float distanceFromPlayer = 35f;
	private float angleAroundPlayer = 0f;
	private float angleBehindPlayer = 0f;

	private BulletPhysicsSystem physicsSystem;
	private MyContactListener myContactListener;
	private btRigidBody playerBody;
	private static final Vector3 direction = new Vector3();
	private static final Vector3 angleChangeVector = new Vector3();

	private Matrix4 tmpMat = new Matrix4();
	public static int PLAYER_FLAG = 2;

	@Override
	public void create() {
		Bullet.init(true);

		physicsSystem = new BulletPhysicsSystem();
		myContactListener = new MyContactListener();
		myContactListener.enable();
		myContactListener.enableOnAdded();

		// create scene
		sceneManager = new SceneManager();

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

		buildPlayer();
		buildBoxes();
		buildGround();
	}

	@Override
	public void resize(int width, int height) {
		sceneManager.updateViewport(width, height);
	}

	@Override
	public void render() {
		float deltaTime = Gdx.graphics.getDeltaTime();
		time += deltaTime;

		physicsSystem.update(deltaTime);

		processInput(deltaTime);
		updateCamera();

		if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
			playerScene.animationController.action("jump", 1, 1f, this, 0.5f);

		// render
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		sceneManager.update(deltaTime);
		sceneManager.render();
		physicsSystem.drawDebug(camera);
	}

	private void processInput(float deltaTime) {
		// Update the player transform
		tmpMat.set(playerScene.modelInstance.transform);

		// Clear the move translation out
		moveTranslation.set(0,0,0);
		angleChangeVector.set(0,0,0);

		direction.set(Vector3.Z);
		Vector3 dir = direction.rot(tmpMat).nor();
		boolean moved = false;
		if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
			Gdx.app.exit();
		}

		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			moveTranslation.set(dir.scl(speed * deltaTime));
			moved = true;
		}

		if (Gdx.input.isKeyPressed(Input.Keys.S)) {
			moveTranslation.set(dir.scl(-speed * deltaTime));
			moved = true;
		}

		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			//playerTransform.rotate(Vector3.Y, rotationSpeed * deltaTime);
			angleChangeVector.y += rotationSpeed * deltaTime;
			angleBehindPlayer += rotationSpeed * deltaTime;
			moved = true;
		}

		if (Gdx.input.isKeyPressed(Input.Keys.D)) {
			//playerTransform.rotate(Vector3.Y, -rotationSpeed * deltaTime);
			angleChangeVector.y -= rotationSpeed * deltaTime;
			angleBehindPlayer -= rotationSpeed * deltaTime;
			moved = true;
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
		//playerTransform.translate(moveTranslation);

		if (moved) {
			playerBody.applyCentralImpulse(moveTranslation);
			playerBody.setAngularVelocity(angleChangeVector);
		}

		// Set the modified transform
		//playerScene.modelInstance.transform.set(playerTransform);

		// Update vector position
		playerScene.modelInstance.transform.getTranslation(currentPosition);
	}

	private void buildPlayer() {
		sceneAsset = new GLTFLoader().load(Gdx.files.internal("models/Alien Slime.gltf"));
		playerScene = new Scene(sceneAsset.scene);
		sceneManager.addScene(playerScene);
		playerScene.animationController.setAnimation("idle", -1);
		playerScene.modelInstance.transform.translate(0, 10f, 0);
		playerBody = physicsSystem.addGimpactBody(playerScene.modelInstance, 10f);

		playerBody.setDamping(.5f, .98f);
		playerBody.setContactCallbackFlag(PLAYER_FLAG);
		playerBody.setAngularFactor(Vector3.Y);
	}

	private void buildBoxes() {

		for (int x = 0; x < 100; x+= 10) {
			for (int z = 0; z < 100; z+= 10) {
				ModelBuilder modelBuilder = new ModelBuilder();
				modelBuilder.begin();
				Material material = new Material();
				material.set(PBRColorAttribute.createBaseColorFactor(Color.RED));
				MeshPartBuilder builder = modelBuilder.part(x + ", " + z, GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, material);
				BoxShapeBuilder.build(builder, 0, 0, 0, 1f,1f,1f);
				ModelInstance modelInstance = new ModelInstance(modelBuilder.end());
				sceneManager.addScene(new Scene(modelInstance));

				modelInstance.transform.setTranslation(x, 100, z);

				btRigidBody body = physicsSystem.addGimpactBody(modelInstance, 5f);
				body.setContactCallbackFilter(PLAYER_FLAG);
			}
		}

	}

	private void buildGround() {
		float size = 200;
		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();

		Material material = new Material(ColorAttribute.createDiffuse(Color.BLUE));
		MeshPartBuilder builder = modelBuilder.part("ground", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, material);
		builder.rect(0, 0, size,
				size, 0, size,
				size, 0, 0,
				0, 0, 0,
				0, 1, 0);

		ModelInstance modelInstance = new ModelInstance(modelBuilder.end());
		modelInstance.transform.translate(-50, 0, -50);
		sceneManager.addScene(new Scene(modelInstance));
		physicsSystem.addGimpactBody(modelInstance, 0f);
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
