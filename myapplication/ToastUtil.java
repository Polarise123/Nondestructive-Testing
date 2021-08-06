package com.kc.myapplication;
import android.content.Context ;
import android.view.View;
import android.widget.Toast;

public class ToastUtil {
    public static Toast mToast;

    public static void shotMsg(View.OnClickListener context, String msg) {
        if (mToast == null) {
            mToast = Toast.makeText((Context) context, msg, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(msg);
            mToast.show();
        }
    }
}