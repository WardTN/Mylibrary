package com.dq.mylibrary;

import android.app.Application;

import androidx.core.content.FileProvider;

import java.util.Objects;

public class DqFileProvider extends FileProvider {
    @Override
    public boolean onCreate() {
        DqAppInstance.INSTANCE.setApplication((Application) Objects.requireNonNull(getContext()).getApplicationContext());
        return true;
    }
}
