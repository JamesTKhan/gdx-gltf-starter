package com.mygdx.game;

public class Settings {
    public static final float CAMERA_START_PITCH = 20f;                     // Default Starting pitch
    public static final float CAMERA_MIN_PITCH = CAMERA_START_PITCH - 20f;  // Min Pitch
    public static final float CAMERA_MAX_PITCH = CAMERA_START_PITCH + 20f;  // Max Pitch
    public static final float CAMERA_PITCH_FACTOR = 0.3f;
    public static final float CAMERA_ZOOM_LEVEL_FACTOR = 0.5f;              // Our zoom multiplier (speed)
    public static final float CAMERA_ANGLE_AROUND_PLAYER_FACTOR = 0.2f;     // Rotation around player speed
    public static final float CAMERA_MIN_DISTANCE_FROM_PLAYER = 4;          // Min zoom distance
}
