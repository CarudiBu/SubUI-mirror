package com.carudibu.android.subuimirror;

import android.app.Presentation;
import android.content.Context;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

public class CoverPresentation extends Presentation {

    public CoverPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);
    }
}

