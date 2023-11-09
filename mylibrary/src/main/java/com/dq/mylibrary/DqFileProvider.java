package com.dq.mylibrary;

import android.app.Application;

import androidx.core.content.FileProvider;

public class DqFileProvider extends FileProvider {
    @Override
    public boolean onCreate() {
        DqAppInstance.INSTANCE.setApplication((Application) getContext().getApplicationContext());
        return true;
    }
}
