package com.example.jjhb;

import de.robv.android.xposed.XC_MethodHook;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class WalletHook {
    public void hook(final ClassLoader classLoader) {

        findAndHookMethod("com.qwallet.qqproxy.j", classLoader, "b", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult("");
                    }
                }
        );


    }
}
