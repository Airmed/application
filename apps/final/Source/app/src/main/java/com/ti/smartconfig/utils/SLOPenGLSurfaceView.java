package com.ti.smartconfig.utils;


import android.content.Context;
import android.opengl.GLSurfaceView;

public class SLOPenGLSurfaceView extends GLSurfaceView
{
    float touchedX = 0;
    float touchedY = 0;
    public SLOpenGLRenderer renderer;
    public SLOPenGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setRenderer(renderer = new SLOpenGLRenderer(this));
    }

    public SLOpenGLRenderer getRenderer() {
        return renderer;
    }

}