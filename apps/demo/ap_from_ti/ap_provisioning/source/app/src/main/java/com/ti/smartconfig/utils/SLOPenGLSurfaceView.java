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

//    @Override
//    public boolean onTouchEvent(MotionEvent event)
//    {
//        if (event.getAction() == MotionEvent.ACTION_DOWN)
//        {
//            touchedX = event.getX();
//            touchedY = event.getY();
//        } else if (event.getAction() == MotionEvent.ACTION_MOVE)
//        {
//            renderer.xAngle += (touchedX - event.getX())/2f;
//            renderer.yAngle += (touchedY - event.getY())/2f;
//
//            touchedX = event.getX();
//            touchedY = event.getY();
//        }
//        return true;
//
//    }


}