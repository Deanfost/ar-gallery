package com.example.deanf.arvideo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.google.ar.core.Pose;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Renders a movie clip with a green screen aware shader.
 * <p>
 * Copyright 2018 Google LLC
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class MovieClipRenderer implements
        SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = MovieClipRenderer.class.getSimpleName();

    // Quad geometry
    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEXCOORDS_PER_VERTEX = 2;
    private static final int FLOAT_SIZE = 4;
    private static final float[] QUAD_COORDS = new float[]{
            -1.0f, -1.0f, 0.0f,
            -1.0f, +1.0f, 0.0f,
            +1.0f, -1.0f, 0.0f,
            +1.0f, +1.0f, 0.0f,
    };

    private static final float[] QUAD_TEXCOORDS = new float[]{
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };

    // Shader for a flat quad.
    private static final String VERTEX_SHADER =
            "uniform mat4 u_ModelViewProjection;\n\n" +
                    "attribute vec4 a_Position;\n" +
                    "attribute vec2 a_TexCoord;\n\n" +
                    "varying vec2 v_TexCoord;\n\n" +
                    "void main() {\n" +
                    "   gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);\n" +
                    "   v_TexCoord = a_TexCoord;\n" +
                    "}";

    // The fragment shader samples the video texture, blending to
    //  transparent for the green screen
    //  color.  The color was determined by sampling a screenshot
    //  of the video in an image editor.
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "\n" +
                    "precision mediump float;\n" +
                    "varying vec2 v_TexCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    //TODO make this a uniform variable - " +
                    " but this is the color of the background. 17ad2b\n" +
                    "  vec3 keying_color = vec3(23.0f/255.0f, 173.0f/255.0f, 43.0f/255.0f);\n" +
                    "  float thresh = 0.4f; // 0 - 1.732\n" +
                    "  float slope = 0.2;\n" +
                    "  vec3 input_color = texture2D(sTexture, v_TexCoord).rgb;\n" +
                    "  float d = abs(length(abs(keying_color.rgb - input_color.rgb)));\n" +
                    "  float edge0 = thresh * (1.0f - slope);\n" +
                    "  float alpha = smoothstep(edge0,thresh,d);\n" +
                    "  gl_FragColor = vec4(input_color, alpha);\n" +
                    "}";

    // Geometry data in GLES friendly data structure.
    private FloatBuffer mQuadVertices;
    private FloatBuffer mQuadTexCoord;

    // Shader program id and parameters.
    private int mQuadProgram;
    private int mQuadPositionParam;
    private int mQuadTexCoordParam;
    private int mModelViewProjectionUniform;
    private int mTextureId = -1;

    // Matrix for the location and perspective of the quad.
    private float[] mModelMatrix = new float[16];

    // Media player,  texture and other bookkeeping.
    private MediaPlayer player;
    private SurfaceTexture videoTexture;
    private boolean frameAvailable = false;
    private boolean started = false;
    private boolean done;
    private boolean prepared;
    private static Handler handler;


    // Lock used for waiting if the player was not yet created.
    private final Object lock = new Object();

    /**
     * Update the model matrix based on the location and scale to draw the quad.
     */
    public void update(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }

    /**
     * Initialize the GLES objects.
     * This is called from the GL render thread to make sure
     * it has access to the EGLContext.
     */
    public void createOnGlThread() {

        // 1 texture to hold the video frame.
        int textures[] = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureId = textures[0];
        int mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        GLES20.glBindTexture(mTextureTarget, mTextureId);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST);

        videoTexture = new SurfaceTexture(mTextureId);
        videoTexture.setOnFrameAvailableListener(this);

        // Make a quad to hold the movie
        ByteBuffer bbVertices = ByteBuffer.allocateDirect(
                QUAD_COORDS.length * FLOAT_SIZE);
        bbVertices.order(ByteOrder.nativeOrder());
        mQuadVertices = bbVertices.asFloatBuffer();
        mQuadVertices.put(QUAD_COORDS);
        mQuadVertices.position(0);

        int numVertices = 4;
        ByteBuffer bbTexCoords = ByteBuffer.allocateDirect(
                numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoords.order(ByteOrder.nativeOrder());
        mQuadTexCoord = bbTexCoords.asFloatBuffer();
        mQuadTexCoord.put(QUAD_TEXCOORDS);
        mQuadTexCoord.position(0);

        int vertexShader = loadGLShader(TAG, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadGLShader(TAG,
                GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        mQuadProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mQuadProgram, vertexShader);
        GLES20.glAttachShader(mQuadProgram, fragmentShader);
        GLES20.glLinkProgram(mQuadProgram);
        GLES20.glUseProgram(mQuadProgram);

//        ShaderUtil.checkGLError(TAG, "Program creation");

        mQuadPositionParam = GLES20.glGetAttribLocation(mQuadProgram, "a_Position");
        mQuadTexCoordParam = GLES20.glGetAttribLocation(mQuadProgram, "a_TexCoord");
        mModelViewProjectionUniform = GLES20.glGetUniformLocation(
                mQuadProgram, "u_ModelViewProjection");

//        ShaderUtil.checkGLError(TAG, "Program parameters");

        Matrix.setIdentityM(mModelMatrix, 0);

        initializeMediaPlayer();
    }

    public void draw(Pose pose, float[] cameraView, float[] cameraPerspective) {
        if (done || !prepared) {
            return;
        }
        synchronized (this) {
            if (frameAvailable) {
                videoTexture.updateTexImage();
                frameAvailable = false;
            }
        }

        float[] modelMatrix = new float[16];
        pose.toMatrix(modelMatrix, 0);

        float[] modelView = new float[16];
        float[] modelViewProjection = new float[16];
        Matrix.multiplyMM(modelView, 0, cameraView, 0, mModelMatrix, 0);
        Matrix.multiplyMM(modelViewProjection, 0, cameraPerspective, 0, modelView, 0);

//        ShaderUtil.checkGLError(TAG, "Before draw");

        GLES20.glEnable(GL10.GL_BLEND);
        GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
        GLES20.glUseProgram(mQuadProgram);

        // Set the vertex positions.
        GLES20.glVertexAttribPointer(
                mQuadPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, mQuadVertices);
        // Set the texture coordinates.
        GLES20.glVertexAttribPointer(mQuadTexCoordParam, TEXCOORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, mQuadTexCoord);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(mQuadPositionParam);
        GLES20.glEnableVertexAttribArray(mQuadTexCoordParam);
        GLES20.glUniformMatrix4fv(mModelViewProjectionUniform, 1, false,
                modelViewProjection, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mQuadPositionParam);
        GLES20.glDisableVertexAttribArray(mQuadTexCoordParam);

//        ShaderUtil.checkGLError(TAG, "Draw");
    }

    private void initializeMediaPlayer() {
        if (handler == null)
            handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    player = new MediaPlayer();
                    lock.notify();
                }
            }
        });
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            frameAvailable = true;
        }
    }

    public boolean play(final String filepath, Context context)
            throws FileNotFoundException {
        // Wait for the player to be created.
        if (player == null) {
            synchronized (lock) {
                while (player == null) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
            }
        }

        player.reset();
        done = false;

        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                prepared = true;
                mp.start();
            }
        });
        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                done = true;
                Log.e("VideoPlayer",
                        String.format("Error occured: %d, %d\n", what, extra));
                return false;
            }
        });

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                done = true;
            }
        });

        player.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
                return false;
            }
        });

        try {
            player.setDataSource(filepath);
            player.setSurface(new Surface(videoTexture));
            player.prepareAsync();
            synchronized (this) {
                started = true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception preparing movie", e);
            return false;
        }

        return true;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    static int loadGLShader(String tag, int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(tag, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }
}