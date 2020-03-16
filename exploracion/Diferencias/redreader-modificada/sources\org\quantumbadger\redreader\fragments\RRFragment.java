package org.quantumbadger.redreader.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public abstract class RRFragment {
    @NonNull
    private final AppCompatActivity mParent;

    public abstract View getView();

    public abstract Bundle onSaveInstanceState();

    protected RRFragment(@NonNull AppCompatActivity parent, Bundle savedInstanceState) {
        this.mParent = parent;
    }

    /* access modifiers changed from: protected */
    @NonNull
    public final Context getContext() {
        return this.mParent;
    }

    /* access modifiers changed from: protected */
    @NonNull
    public final AppCompatActivity getActivity() {
        return this.mParent;
    }

    /* access modifiers changed from: protected */
    public final String getString(int resource) {
        return this.mParent.getApplicationContext().getString(resource);
    }

    /* access modifiers changed from: protected */
    public final void startActivity(Intent intent) {
        this.mParent.startActivity(intent);
    }

    /* access modifiers changed from: protected */
    public final void startActivityForResult(Intent intent, int requestCode) {
        this.mParent.startActivityForResult(intent, requestCode);
    }

    public void onCreateOptionsMenu(Menu menu) {
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }
}
