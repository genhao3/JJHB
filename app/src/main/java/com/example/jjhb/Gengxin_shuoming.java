package com.example.jjhb;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class Gengxin_shuoming extends DialogPreference {
    public Gengxin_shuoming(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.gengxin_shuoming );


    }
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
    }
}
