package com.example.jjhb;

public class VersionParam {

    public static String QQPluginClass = "com.tenpay.android.qqplugin.a.q";
    public static String walletPluginClass = "com.tenpay.android.qqplugin.c.d";
    public static String pickObject = "b";

    public static void init(int version) {
        if (version < 260) {
            QQPluginClass = "com.tenpay.android.qqplugin.a.o";
            walletPluginClass = "com.tenpay.android.qqplugin.b.d";
            pickObject = "a";
        } else if (version <= 312) {
            QQPluginClass = "com.tenpay.android.qqplugin.a.p";
            walletPluginClass = "com.tenpay.android.qqplugin.b.d";
            pickObject = "a";
        } else if (version <= 482) {
            QQPluginClass = "com.tenpay.android.qqplugin.a.q";
            walletPluginClass = "com.tenpay.android.qqplugin.b.d";
            pickObject = "a";
        } else if (version <= 496) {
            QQPluginClass = "com.tenpay.android.qqplugin.a.q";
            walletPluginClass = "com.tenpay.android.qqplugin.b.d";
            pickObject = "b";
        } else if (version <= 500) {
            QQPluginClass = "com.tenpay.android.qqplugin.a.q";
            walletPluginClass = "com.tenpay.android.qqplugin.c.d";
            pickObject = "b";
        } else {
            QQPluginClass = "com.tenpay.android.qqplugin.a.q";
            walletPluginClass = "com.tenpay.android.qqplugin.c.d";
            pickObject = "b";
        }
    }
}
