/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

package com.qualcomm.vuforia.samples.VuforiaSamples.app.MultiTargets;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.qualcomm.vuforia.*;
import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.qualcomm.vuforia.samples.SampleApplication.utils.CubeObject;
import com.qualcomm.vuforia.samples.SampleApplication.utils.CubeShaders;
import com.qualcomm.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.qualcomm.vuforia.samples.SampleApplication.utils.Texture;


// The renderer class for the MultiTargets sample. 
public class MultiTargetRenderer implements GLSurfaceView.Renderer
{
    private static String LOGTAG = "MultiTargetRenderer";
    static int NUM_QUAD_INDEX = 6;

    private MultiTargets mActivity;
    private VideoPlayerHelper mVideoPlayerHelper;
    private String mMovieName;
    private VideoPlayerHelper.MEDIA_TYPE mCanRequestType;
    private int mSeekPosition;
    private boolean mShouldPlayImmediately;
    private long mLostTrackingSince;
    private boolean mLoadRequested;
    public Vec2F targetPositiveDimensions;
    private Matrix44F modelViewMatrix;
    private float[] mTexCoordTransformationMatrix = null;

    private float videoQuadTextureCoordsTransformedStones[] = { 0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, };
    private float videoQuadTextureCoords[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f, };

    double quadVerticesArray[] = { -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, -1.0f, 1.0f, 0.0f };

    double quadTexCoordsArray[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
            1.0f };

    double quadNormalsArray[] = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, };

    short quadIndicesArray[] = { 0, 1, 2, 2, 3, 0 };

    Buffer quadVertices, quadTexCoords, quadIndices, quadNormals;

    private SampleApplicationSession vuforiaAppSession;
    
    boolean mIsActive = false;
    
    private int shaderProgramID;
    
    private int vertexHandle;
    private int normalHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;
    
    private Vector<Texture> mTextures;
    
    private double prevTime;
    private float rotateAngle;
    
    private CubeObject cubeObject = new CubeObject();
    private BowlAndSpoonObject bowlAndSpoonObject = new BowlAndSpoonObject();
    
    // Constants:
    final static float kCubeScaleX = 120.0f * /*0.75f*/1.5f / 2.0f;
    final static float kCubeScaleY = 120.0f * 1.00f / 2.0f;
    final static float kCubeScaleZ = 120.0f * 0.50f / 2.0f;
    
    final static float kBowlScaleX = 120.0f * 0.15f;
    final static float kBowlScaleY = 120.0f * 0.15f;
    final static float kBowlScaleZ = 120.0f * 0.15f;
    public int[] videoPlaybackTextureID = new int[1];
    private float videoQuadAspectRatio;
    private VideoPlayerHelper.MEDIA_STATE currentStatus;
    private boolean isTracking;
    private int videoPlaybackShaderID;
    private int videoPlaybackVertexHandle = 0;
    private int videoPlaybackNormalHandle = 0;
    private int videoPlaybackTexCoordHandle = 0;
    private int videoPlaybackMVPMatrixHandle = 0;
    private int videoPlaybackTexSamplerOESHandle = 0;


    public MultiTargetRenderer(MultiTargets activity, SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;


        mVideoPlayerHelper = null;
        mMovieName = "";
        mCanRequestType = VideoPlayerHelper.MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
        mSeekPosition = 0;
        mShouldPlayImmediately = false;
        mLostTrackingSince = -1;
        mLoadRequested = false;
        mTexCoordTransformationMatrix = new float[16];

        targetPositiveDimensions = new Vec2F();

        modelViewMatrix = new Matrix44F();
    }

    public void setVideoPlayerHelper(VideoPlayerHelper newVideoPlayerHelper)
    {
        mVideoPlayerHelper = newVideoPlayerHelper;
    }

    public void requestLoad(String movieName, int seekPosition, boolean playImmediately)
    {
        mMovieName = movieName;
        mSeekPosition = seekPosition;
        mShouldPlayImmediately = playImmediately;
        mLoadRequested = true;
    }
    
    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        
        // Call function to initialize rendering:
        initRendering();
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        if (mVideoPlayerHelper != null)
        {
            // The VideoPlayerHelper needs to setup a surface texture given
            // the texture id
            // Here we inform the video player that we would like to play
            // the movie
            // both on texture and on full screen
            // Notice that this does not mean that the platform will be able
            // to do what we request
            // After the file has been loaded one must always check with
            // isPlayableOnTexture() whether
            // this can be played embedded in the AR scene
            if (mVideoPlayerHelper.setupSurfaceTexture(videoPlaybackTextureID[0]))
                mCanRequestType = VideoPlayerHelper.MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;

            // And now check if a load has been requested with the
            // parameters passed from the main activity
            if (mLoadRequested)
            {
                mVideoPlayerHelper.load(mMovieName, mCanRequestType, mShouldPlayImmediately, mSeekPosition);
                mLoadRequested = false;
            }
        }
    }
    
    
    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        if (mLoadRequested && mVideoPlayerHelper != null)
        {
            mVideoPlayerHelper.load(mMovieName, mCanRequestType, mShouldPlayImmediately, mSeekPosition);
            mLoadRequested = false;
        }
    }
    
    
    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        if (mVideoPlayerHelper.isPlayableOnTexture())
        {
            // First we need to update the video data. This is a built
            // in Android call
            // Here, the decoded data is uploaded to the OES texture
            // We only need to do this if the movie is playing
            if (mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING)
            {
                mVideoPlayerHelper.updateVideoData();
            }

            // According to the Android API
            // (http://developer.android.com/reference/android/graphics/SurfaceTexture.html)
            // transforming the texture coordinates needs to happen
            // every frame.
            mVideoPlayerHelper.getSurfaceTextureTransformMatrix(mTexCoordTransformationMatrix);
            setVideoDimensions(mVideoPlayerHelper.getVideoWidth(),
                    mVideoPlayerHelper.getVideoHeight(),
                    mTexCoordTransformationMatrix);
        }

        setStatus(mVideoPlayerHelper.getStatus().getNumericType());
        
        // Call our function to render content:
        renderFrame();

        if (isTracking())
        {
            // If it is tracking reset the timestamp for lost tracking
            mLostTrackingSince = -1;
        } else
        {
            // If it isn't tracking
            // check whether it just lost it or if it's been a while
            if (mLostTrackingSince < 0)
                mLostTrackingSince = SystemClock.uptimeMillis();
            else
            {
                // If it's been more than 2 seconds then pause the player
                if ((SystemClock.uptimeMillis() - mLostTrackingSince) > 2000)
                {
                    if (mVideoPlayerHelper != null)
                        mVideoPlayerHelper.pause();
                }
            }
        }
    }

    void setStatus(int value)
    {
        // Transform the value passed from java to our own values
        switch (value)
        {
            case 0:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.REACHED_END;
                break;
            case 1:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.PAUSED;
                break;
            case 2:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.STOPPED;
                break;
            case 3:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.PLAYING;
                break;
            case 4:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.READY;
                break;
            case 5:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
            case 6:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.ERROR;
                break;
            default:
                currentStatus = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
        }
    }

    void setVideoDimensions(float videoWidth, float videoHeight, float[] textureCoordMatrix)
    {
        // The quad originaly comes as a perfect square, however, the video
        // often has a different aspect ration such as 4:3 or 16:9,
        // To mitigate this we have two options:
        // 1) We can either scale the width (typically up)
        // 2) We can scale the height (typically down)
        // Which one to use is just a matter of preference. This example scales
        // the height down.
        // (see the render call in renderFrame)
        videoQuadAspectRatio = videoHeight / videoWidth;

        float mtx[] = textureCoordMatrix;
        float tempUVMultRes[] = new float[2];

        tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedStones[0],
                videoQuadTextureCoordsTransformedStones[1],
                videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
        videoQuadTextureCoordsTransformedStones[0] = tempUVMultRes[0];
        videoQuadTextureCoordsTransformedStones[1] = tempUVMultRes[1];
        tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedStones[2],
                videoQuadTextureCoordsTransformedStones[3],
                videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
        videoQuadTextureCoordsTransformedStones[2] = tempUVMultRes[0];
        videoQuadTextureCoordsTransformedStones[3] = tempUVMultRes[1];
        tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedStones[4],
                videoQuadTextureCoordsTransformedStones[5],
                videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
        videoQuadTextureCoordsTransformedStones[4] = tempUVMultRes[0];
        videoQuadTextureCoordsTransformedStones[5] = tempUVMultRes[1];
        tempUVMultRes = uvMultMat4f(
                videoQuadTextureCoordsTransformedStones[6],
                videoQuadTextureCoordsTransformedStones[7],
                videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
        videoQuadTextureCoordsTransformedStones[6] = tempUVMultRes[0];
        videoQuadTextureCoordsTransformedStones[7] = tempUVMultRes[1];

        // textureCoordMatrix = mtx;
    }

    float[] uvMultMat4f(float transformedU, float transformedV, float u,
                        float v, float[] pMat)
    {
        float x = pMat[0] * u + pMat[4] * v /* + pMat[ 8]*0.f */+ pMat[12]
                * 1.f;
        float y = pMat[1] * u + pMat[5] * v /* + pMat[ 9]*0.f */+ pMat[13]
                * 1.f;
        // float z = pMat[2]*u + pMat[6]*v + pMat[10]*0.f + pMat[14]*1.f; // We
        // dont need z and w so we comment them out
        // float w = pMat[3]*u + pMat[7]*v + pMat[11]*0.f + pMat[15]*1.f;

        float result[] = new float[2];
        // transformedU = x;
        // transformedV = y;
        result[0] = x;
        result[1] = y;
        return result;
    }
    
    
    private void initRendering()
    {
        Log.d(LOGTAG, "MultiTargetsRenderer.initRendering");
        
        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);
        
        // Now generate the OpenGL texture objects and add settings
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, t.mWidth, t.mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

//        GLES20.glGenTextures(1, videoPlaybackTextureID, 0);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoPlaybackTextureID[0]);
//        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        
        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexPosition");
        normalHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexNormal");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "modelViewProjectionMatrix");
//        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSampler2D");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSamplerOES");

        // Now we create the texture for the video data from the movie
        // IMPORTANT:
        // Notice that the textures are not typical GL_TEXTURE_2D textures
        // but instead are GL_TEXTURE_EXTERNAL_OES extension textures
        // This is required by the Android SurfaceTexture
        GLES20.glGenTextures(1, videoPlaybackTextureID, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoPlaybackTextureID[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        // The first shader is the one that will display the video data of the
        // movie
        // (it is aware of the GL_TEXTURE_EXTERNAL_OES extension)
        videoPlaybackShaderID = SampleUtils.createProgramFromShaderSrc(
                VideoPlaybackShaders.VIDEO_PLAYBACK_VERTEX_SHADER,
                VideoPlaybackShaders.VIDEO_PLAYBACK_FRAGMENT_SHADER);
        videoPlaybackVertexHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexPosition");
        videoPlaybackNormalHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexNormal");
        videoPlaybackTexCoordHandle = GLES20.glGetAttribLocation(
                videoPlaybackShaderID, "vertexTexCoord");
        videoPlaybackMVPMatrixHandle = GLES20.glGetUniformLocation(
                videoPlaybackShaderID, "modelViewProjectionMatrix");
        videoPlaybackTexSamplerOESHandle = GLES20.glGetUniformLocation(
                videoPlaybackShaderID, "texSamplerOES");

        quadVertices = fillBuffer(quadVerticesArray);
        quadTexCoords = fillBuffer(quadTexCoordsArray);
        quadIndices = fillBuffer(quadIndicesArray);
        quadNormals = fillBuffer(quadNormalsArray);
    }
    
    
    private void renderFrame()
    {
        SampleUtils.checkGLError("Check gl errors prior render Frame");
        
        // Clear color and depth buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        // Get the state from Vuforia and mark the beginning of a rendering
        // section
        State state = Renderer.getInstance().begin();
        
        // Explicitly render the Video Background
        Renderer.getInstance().drawVideoBackground();
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        float temp[] = { 0.0f, 0.0f };
        isTracking = false;
        targetPositiveDimensions.setData(temp);

        // Did we find any trackables this frame?
        if (state.getNumTrackableResults() != 0)
        {
            // Get the trackable:
            TrackableResult trackableResult = null;
            int numResults = state.getNumTrackableResults();
            
            // Browse results searching for the MultiTarget
            for (int j = 0; j < numResults; j++)
            {
                trackableResult = state.getTrackableResult(j);
                if (trackableResult.isOfType(MultiTargetResult.getClassType()))
                    break;
                trackableResult = null;
            }
            
            // If it was not found exit
            if (trackableResult == null)
            {
                // Clean up and leave
                GLES20.glDisable(GLES20.GL_BLEND);
                GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                
                Renderer.getInstance().end();
                return;
            }

            if ((mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.PAUSED)
                    || (mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.READY)
                    || (mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.STOPPED)
                    || (mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.REACHED_END))
            {
                // If it has reached the end then rewind
                if ((mVideoPlayerHelper.getStatus() == VideoPlayerHelper.MEDIA_STATE.REACHED_END))
                    mSeekPosition = 0;

                mVideoPlayerHelper.play(false, mSeekPosition);
                mSeekPosition = VideoPlayerHelper.CURRENT_POSITION;
            }

            Matrix44F modelViewMatrix_Vuforia = Tool
                .convertPose2GLMatrix(trackableResult.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

            isTracking = true;


            float[] modelViewProjection = new float[16];
            Matrix.scaleM(modelViewMatrix, 0, kCubeScaleX, kCubeScaleX * videoQuadAspectRatio, kCubeScaleZ);
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrix, 0);
            
            GLES20.glUseProgram(shaderProgramID);
            
            // Draw the cube:
            
            // We must detect if background reflection is active and adjust the
            // culling direction.
            // If the reflection is active, this means the post matrix has been
            // reflected as well, therefore standard counter clockwise face
            // culling will trackableResult in "inside out" models.
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
                GLES20.glFrontFace(GLES20.GL_CW); // Front camera
            else
                GLES20.glFrontFace(GLES20.GL_CCW); // Back camera
                
//            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, cubeObject.getVertices());
//            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, cubeObject.getNormals());
//            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, cubeObject.getTexCoords());

            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, quadVertices);
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, quadNormals);
            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, /*quadTexCoords*/fillBuffer(videoQuadTextureCoordsTransformedStones));
            
            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);
            
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, /*mTextures.get(0).mTextureID[0]*/videoPlaybackTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
            GLES20.glUniform1i(texSampler2DHandle, 0);
//            GLES20.glDrawElements(GLES20.GL_TRIANGLES, cubeObject.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT, cubeObject.getIndices());
//            GLES20.glDrawElements(GLES20.GL_TRIANGLES, cubeObject.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT, quadIndices);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, quadIndicesArray.length, GLES20.GL_UNSIGNED_SHORT, quadIndices);

            /*///////////////////////////////////////////////////////
            if ((currentStatus == VideoPlayerHelper.MEDIA_STATE.READY)
                    || (currentStatus == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
                    || (currentStatus == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
                    || (currentStatus == VideoPlayerHelper.MEDIA_STATE.ERROR))
            {

            } else {
                float[] modelViewMatrixVideo = Tool.convertPose2GLMatrix(trackableResult.getPose()).getData();
                float[] modelViewProjectionVideo = new float[16];
                // Matrix.translateM(modelViewMatrixVideo, 0, 0.0f, 0.0f,
                // targetPositiveDimensions[currentTarget].getData()[0]);

                // Here we use the aspect ratio of the video frame
                Matrix.scaleM(modelViewMatrixVideo, 0, kCubeScaleX, kCubeScaleX * videoQuadAspectRatio, kCubeScaleZ);
                Matrix.multiplyMM(modelViewProjectionVideo, 0, vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrixVideo, 0);

                GLES20.glDepthFunc(GLES20.GL_LEQUAL);
    //            GLES20.glEnable(GLES20.GL_BLEND);
    //            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                GLES20.glUseProgram(videoPlaybackShaderID);

                SampleUtils.checkGLError("MultiTargets glUseProgram");

                // Prepare for rendering the keyframe
                GLES20.glVertexAttribPointer(videoPlaybackVertexHandle, 3, GLES20.GL_FLOAT, false, 0, quadVertices);
                GLES20.glVertexAttribPointer(videoPlaybackNormalHandle, 3, GLES20.GL_FLOAT, false, 0, quadNormals);

                GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, fillBuffer(videoQuadTextureCoordsTransformedStones));

                SampleUtils.checkGLError("MultiTargets Vertex attrib pointer");

                GLES20.glEnableVertexAttribArray(videoPlaybackVertexHandle);
                GLES20.glEnableVertexAttribArray(videoPlaybackNormalHandle);
                GLES20.glEnableVertexAttribArray(videoPlaybackTexCoordHandle);

                SampleUtils.checkGLError("MultiTargets Vertex attrib array");

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

                SampleUtils.checkGLError("MultiTargets active texture 0");

                // IMPORTANT:
                // Notice here that the texture that we are binding is not the
                // typical GL_TEXTURE_2D but instead the GL_TEXTURE_EXTERNAL_OES
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoPlaybackTextureID[0]);
                SampleUtils.checkGLError("MultiTargets bind texture");
                GLES20.glUniformMatrix4fv(videoPlaybackMVPMatrixHandle, 1, false, modelViewProjectionVideo, 0);
                SampleUtils.checkGLError("MultiTargets uniform matrix");
                GLES20.glUniform1i(videoPlaybackTexSamplerOESHandle, 0);

                SampleUtils.checkGLError("MultiTargets uniform 1i");

                // Render
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX, GLES20.GL_UNSIGNED_SHORT, quadIndices);
                SampleUtils.checkGLError("MultiTargets draw elements");

                GLES20.glDisableVertexAttribArray(videoPlaybackVertexHandle);
                GLES20.glDisableVertexAttribArray(videoPlaybackNormalHandle);
                GLES20.glDisableVertexAttribArray(videoPlaybackTexCoordHandle);

                SampleUtils.checkGLError("MultiTargets disable Vertext attrib array");

    //            GLES20.glDisable(GLES20.GL_BLEND);

                GLES20.glUseProgram(0);
                GLES20.glDepthFunc(GLES20.GL_LESS);
            }
            ///////////////////////////////////////////////////////*/
            
            /*GLES20.glDisable(GLES20.GL_CULL_FACE);
            
            // Draw the bowl:
            modelViewMatrix = modelViewMatrix_Vuforia.getData();
            
            // Remove the following line to make the bowl stop spinning:
            animateBowl(modelViewMatrix);
            
            Matrix.translateM(modelViewMatrix, 0, 0.0f, -0.50f * 120.0f,
                1.35f * 120.0f);
            Matrix.rotateM(modelViewMatrix, 0, -90.0f, 1.0f, 0, 0);
            
            Matrix.scaleM(modelViewMatrix, 0, kBowlScaleX, kBowlScaleY,
                kBowlScaleZ);
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);
            
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, bowlAndSpoonObject.getVertices());
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                false, 0, bowlAndSpoonObject.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, bowlAndSpoonObject.getTexCoords());
            
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(1).mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                bowlAndSpoonObject.getNumObjectIndex(),
                GLES20.GL_UNSIGNED_SHORT, bowlAndSpoonObject.getIndices());
            
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);

            GLES20.glUseProgram(0);*/

            ///////////////////////////////////////////////////////

//            GLES20.glEnable(GLES20.GL_CULL_FACE);
//            GLES20.glCullFace(GLES20.GL_BACK);
//            if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
//                GLES20.glFrontFace(GLES20.GL_CW); // Front camera
//            else
//                GLES20.glFrontFace(GLES20.GL_CCW); // Back camera
//
//            if ((currentStatus == VideoPlayerHelper.MEDIA_STATE.READY)
//                    || (currentStatus == VideoPlayerHelper.MEDIA_STATE.REACHED_END)
//                    || (currentStatus == VideoPlayerHelper.MEDIA_STATE.NOT_READY)
//                    || (currentStatus == VideoPlayerHelper.MEDIA_STATE.ERROR))
//            {
//
//            } else {
//                float[] modelViewMatrixVideo = Tool.convertPose2GLMatrix(trackableResult.getPose()).getData();
//                float[] modelViewProjectionVideo = new float[16];
//                // Matrix.translateM(modelViewMatrixVideo, 0, 0.0f, 0.0f,
//                // targetPositiveDimensions[currentTarget].getData()[0]);
//
//                // Here we use the aspect ratio of the video frame
//                Matrix.scaleM(modelViewMatrixVideo, 0, kCubeScaleX, kCubeScaleX * videoQuadAspectRatio, kCubeScaleZ);
//                Matrix.multiplyMM(modelViewProjectionVideo, 0, vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrixVideo, 0);
//
//                GLES20.glDepthFunc(GLES20.GL_LEQUAL);
//                //            GLES20.glEnable(GLES20.GL_BLEND);
//                //            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//
//                GLES20.glUseProgram(videoPlaybackShaderID);
//
//                SampleUtils.checkGLError("MultiTargets glUseProgram");
//
//                // Prepare for rendering the keyframe
//                GLES20.glVertexAttribPointer(videoPlaybackVertexHandle, 3, GLES20.GL_FLOAT, false, 0, quadVertices);
//                GLES20.glVertexAttribPointer(videoPlaybackNormalHandle, 3, GLES20.GL_FLOAT, false, 0, quadNormals);
//
//                GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, fillBuffer(videoQuadTextureCoordsTransformedStones));
//
//                SampleUtils.checkGLError("MultiTargets Vertex attrib pointer");
//
//                GLES20.glEnableVertexAttribArray(videoPlaybackVertexHandle);
//                GLES20.glEnableVertexAttribArray(videoPlaybackNormalHandle);
//                GLES20.glEnableVertexAttribArray(videoPlaybackTexCoordHandle);
//
//                SampleUtils.checkGLError("MultiTargets Vertex attrib array");
//
//                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//
//                SampleUtils.checkGLError("MultiTargets active texture 0");
//
//                // IMPORTANT:
//                // Notice here that the texture that we are binding is not the
//                // typical GL_TEXTURE_2D but instead the GL_TEXTURE_EXTERNAL_OES
//                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoPlaybackTextureID[0]);
//                SampleUtils.checkGLError("MultiTargets bind texture");
//                GLES20.glUniformMatrix4fv(videoPlaybackMVPMatrixHandle, 1, false, modelViewProjectionVideo, 0);
//                SampleUtils.checkGLError("MultiTargets uniform matrix");
//                GLES20.glUniform1i(videoPlaybackTexSamplerOESHandle, 0);
//
//                SampleUtils.checkGLError("MultiTargets uniform 1i");
//
//                // Render
//                GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX, GLES20.GL_UNSIGNED_SHORT, quadIndices);
//                SampleUtils.checkGLError("MultiTargets draw elements");
//
//                GLES20.glDisableVertexAttribArray(videoPlaybackVertexHandle);
//                GLES20.glDisableVertexAttribArray(videoPlaybackNormalHandle);
//                GLES20.glDisableVertexAttribArray(videoPlaybackTexCoordHandle);
//
//                SampleUtils.checkGLError("MultiTargets disable Vertext attrib array");
//
//                //            GLES20.glDisable(GLES20.GL_BLEND);
//
//                GLES20.glUseProgram(0);
//                GLES20.glDepthFunc(GLES20.GL_LESS);
//            }
            ///////////////////////////////////////////////////////
            
            SampleUtils.checkGLError("MultiTargets renderFrame");
            
        }
        
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        Renderer.getInstance().end();
        
    }
    
    
    private void animateBowl(float[] modelViewMatrix)
    {
        double time = System.currentTimeMillis(); // Get real time difference
        float dt = (float) (time - prevTime) / 1000; // from frame to frame
        
        rotateAngle += dt * 180.0f / 3.1415f; // Animate angle based on time
        rotateAngle %= 360;
        Log.d(LOGTAG, "Delta animation time: " + rotateAngle);
        
        Matrix.rotateM(modelViewMatrix, 0, rotateAngle, 0.0f, 1.0f, 0.0f);
        
        prevTime = time;
    }

    boolean isTracking()
    {
        return isTracking;
    }

    private Buffer fillBuffer(double[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
        // float
        // takes 4
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind();

        return bb;

    }


    private Buffer fillBuffer(short[] array)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length); // each
        // short
        // takes 2
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : array)
            bb.putShort(s);
        bb.rewind();

        return bb;

    }


    private Buffer fillBuffer(float[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
        // float
        // takes 4
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();

        return bb;

    }
    
    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
        
    }
    
}
