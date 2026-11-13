package com.pyramidsoft.siqasir.utils;

import android.content.Context;
import android.widget.Toast;

public class GlobalToast {

    public static void ShowToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

    }
}
