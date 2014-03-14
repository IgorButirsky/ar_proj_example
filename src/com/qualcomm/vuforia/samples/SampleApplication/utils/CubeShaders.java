/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

package com.qualcomm.vuforia.samples.SampleApplication.utils;

public class CubeShaders
{
    
    public static final String CUBE_MESH_VERTEX_SHADER = " \n" + "\n"
        + "attribute vec4 vertexPosition; \n"
        + "attribute vec4 vertexNormal; \n"
        + "attribute vec2 vertexTexCoord; \n" + "\n"
        + "varying vec2 texCoord; \n"
        + "varying vec4 normal; \n" + "\n"
        + "uniform mat4 modelViewProjectionMatrix; \n" + "\n"
        + "void main() \n" + "{ \n"
        + "   gl_Position = modelViewProjectionMatrix * vertexPosition; \n"
        + "   normal = vertexNormal; \n"
        + "   texCoord = vertexTexCoord; \n"
        + "} \n";

    public static final String CUBE_MESH_FRAGMENT_SHADER = " \n"
            + "#extension GL_OES_EGL_image_external : require \n"
            +"precision mediump float;\n"
            +"varying vec2 texCoord;\n"

            +"uniform samplerExternalOES texSamplerOES; \n" + " \n"

            +"void main()\n"
            +"{     \n"
            +"  highp float r;\n"
            +"  highp float g;\n"
            +"  highp float b;\n"
            +"  highp float a;\n"

            +"  r=texture2D(texSamplerOES,texCoord).r;\n"
            +"  g=texture2D(texSamplerOES,texCoord).g;\n"
            +"  b=texture2D(texSamplerOES,texCoord).b;   \n"
            +"  if( r>0.28 && r<0.52 && g> 0.48 && g< 0.62 && b > 0.18 && b < 0.32  ) {\n"
            +"      a=0.0;\n"
            +"  } else {\n"
            +"      a=1.0;\n"
            +"  }\n"
            +"  gl_FragColor = vec4(r,g,b,a);\n"
            +"}\n";
    
//    public static final String CUBE_MESH_FRAGMENT_SHADER = " \n" + "\n"
//        + "precision mediump float; \n" + " \n"
//        + "varying vec2 texCoord; \n"
//        + "varying vec4 normal; \n" + " \n"
//        + "uniform sampler2D texSampler2D; \n" + " \n"
//        + "void main() \n"
//        + "{ \n"
//        + "   gl_FragColor = texture2D(texSampler2D, texCoord); \n"
//        + "} \n";
    
}
