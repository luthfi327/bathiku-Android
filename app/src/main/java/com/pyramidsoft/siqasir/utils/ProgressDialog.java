package com.pyramidsoft.siqasir.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;

import com.pyramidsoft.siqasir.R;

public class ProgressDialog {

    public AlertDialog.Builder builder;
    public Dialog dialog;
    public Context context;

    public ProgressDialog(Context context) {
        this.context = context;
        this.builder = new AlertDialog.Builder(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.builder.setView(R.layout.progress);
        }
        this.dialog = builder.create();
        this.dialog.setCancelable(false);
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
