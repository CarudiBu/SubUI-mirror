package com.carudibu.android.subuimirror;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;

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

