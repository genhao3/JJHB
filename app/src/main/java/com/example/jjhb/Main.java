package com.example.jjhb;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import dalvik.system.BaseDexClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.os.SystemClock.sleep;
import static android.widget.Toast.LENGTH_LONG;
import static com.example.jjhb.HideModule.hideModule;
import static com.example.jjhb.XposedUtils.findFieldByClassAndTypeAndName;
import static com.example.jjhb.XposedUtils.findResultByMethodNameAndReturnTypeAndParams;
import static com.example.jjhb.enums.PasswordStatus.CLOSE;
import static com.example.jjhb.enums.PasswordStatus.SEND;
import static com.example.jjhb.enums.ReplyStatus.ALL;
import static com.example.jjhb.enums.ReplyStatus.GOT;
import static com.example.jjhb.enums.ReplyStatus.MISSED;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFirstFieldByExactType;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;
import static java.lang.String.valueOf;

public class Main implements IXposedHookLoadPackage {
    public static final String QQ_PACKAGE_NAME = "com.tencent.mobileqq";
    public static final String REQUEST_UTIL = "com.tenpay.sdk.d.j";

    public static final String REQUEST_CALLER = "com.tenpay.sdk.g.g";

    private static long msgUid;
    private static String senderuin;//发送者Q号
    private static String frienduin;//群号
    private static String from;
    private static int istroop;//1群，5热聊，3000讨论组，私聊
    private static String selfuin;//我的Q号
    private static String msgContext;//消息内容
    private static Context globalContext;
    private static Object HotChatManager;
    private static Object TicketManager;
    private static Object TroopManager;
    private static Object DiscussionManager;
    private static Object FriendManager;
    private static Object globalQQInterface = null;
    private static Object SessionInfo;
    private static Object messageParam;
    private static int n = 1;


    private void dohook(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        new DonateHook().hook(loadPackageParam);

        findAndHookConstructor("dalvik.system.BaseDexClassLoader", loadPackageParam.classLoader, String.class, File.class, String.class, ClassLoader.class, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0].toString().endsWith("qwallet_plugin.apk")) {
                            new WalletHook().hook((BaseDexClassLoader) param.thisObject);
                        }
                    }
                }
        );

        findAndHookMethod("com.tencent.mobileqq.data.MessageForQQWalletMsg", loadPackageParam.classLoader, "doParse", new
                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!PreferencesUtils.open() || msgUid == 0) {
                            return;
                        }
                        msgUid = 0;

                        int messageType = (int) getObjectField(param.thisObject, "messageType");
                        XposedBridge.log("messageType:" + messageType);
                        if (messageType == 6 && PreferencesUtils.password() == CLOSE) {
                            return;
                        }
                        if (13 == messageType) {
                            return;
                        }

                        Object mQQWalletRedPacketMsg = getObjectField(param.thisObject, "mQQWalletRedPacketMsg");
                        String redPacketId = getObjectField(mQQWalletRedPacketMsg, "redPacketId").toString();
                        String authkey = (String) getObjectField(mQQWalletRedPacketMsg, "authkey");
                        SessionInfo = newInstance(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader));
                        findFieldByClassAndTypeAndName(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader), String.class, "a").set(SessionInfo, frienduin);
                        findFieldByClassAndTypeAndName(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader), Integer.TYPE, "a").setInt(SessionInfo, istroop);
                        Object QQWalletTransferMsgElem = XposedHelpers.getObjectField(mQQWalletRedPacketMsg, "elem");
                        String password = XposedHelpers.getObjectField(QQWalletTransferMsgElem, "title").toString();
                        messageParam = newInstance(findClass("com.tencent.mobileqq.activity.ChatActivityFacade$SendMsgParams", loadPackageParam.classLoader));

                        if (selfuin.equals(senderuin) && PreferencesUtils.self()) {
                            return;//自己发的不抢
                        }

                        String group = PreferencesUtils.group();
                        if (!TextUtils.isEmpty(group)) {
                            for (String group1 : group.split(",")) {
                                if (frienduin.equals(group1) || senderuin.equals(group1)) {
                                    if (istroop == 1 && senderuin.equals(group1)) {
                                        from = "指定人不抢" + "\n" + "来自群:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", frienduin), "troopname") + "\n" + "来自:" + getObjectField(callMethod(FriendManager, "c", group1), "name");
                                    } else if (istroop == 1) {
                                        from = "指定群不抢" + "\n" + "来自群:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", group1), "troopname");
                                    } else {
                                        from = "指定人不抢" + "\n" + "来自:" + getObjectField(callMethod(FriendManager, "c", group1), "name");
                                    }
                                    toast(from);
                                    return;
                                }
                            }
                        }

                        String keywords = PreferencesUtils.keywords();
                        if (!TextUtils.isEmpty(keywords)) {
                            for (String keywords1 : keywords.split(",")) {
                                if (password.contains(keywords1)) {
                                    toast("关键词不抢" + "\n" + "关键词:" + keywords1);
                                    return;
                                }
                            }
                        }


                        if (PreferencesUtils.delay()) {//延迟
                            sleep(PreferencesUtils.delayTime());
                        }

                        ClassLoader walletClassLoader = (ClassLoader) callStaticMethod(findClass("com.tencent.mobileqq.pluginsdk.PluginStatic", loadPackageParam.classLoader), "getOrCreateClassLoader", globalContext, "qwallet_plugin.apk");
                        Object requestCaller = newInstance(findClass(REQUEST_CALLER, walletClassLoader), new Object[]{null});

                        String skey = (String) callMethod(TicketManager, "getSkey", selfuin);
                        int random = Math.abs(new Random().nextInt()) % 16;
                        StringBuffer requestUrl = new StringBuffer()
                                .append("uin").append("=").append(selfuin)
                                .append("&").append("listid").append("=").append(redPacketId)
                                .append("&").append("authkey").append("=").append(authkey)
                                .append("&").append("skey_type").append("=").append(2)
                                .append("&").append("groupid").append("=").append(istroop == 0 ? selfuin : frienduin)
                                .append("&").append("grouptype").append("=").append(getGroupType())
                                .append("&").append("groupuin").append("=").append(getGroupuin(messageType))
                                .append("&").append("name").append("=").append(getObjectField(callMethod(FriendManager, "c", selfuin), "name"))
                                .append("&").append("skey").append("=").append(skey)
                                .append("&").append("channel").append("=").append(getObjectField(mQQWalletRedPacketMsg, "redChannel"));
                        if (13 != messageType) {
                            requestUrl.append("&").append("senderuin").append("=").append(senderuin)
                                    .append("&").append("hb_from").append("=").append(0);

                        }
                        requestUrl.append("&").append("agreement").append("=").append(0);

                        String reqText = (String) callStaticMethod(findClass(REQUEST_UTIL, walletClassLoader), "a", globalContext, "https://mqq.tenpay.com/cgi-bin/hongbao/qpay_hb_na_grap.cgi?ver=2.0&chv=3", random, requestUrl.toString());

                        String openLuckyMoneyUrl = new StringBuffer("https://mqq.tenpay.com/cgi-bin/hongbao/qpay_hb_na_grap.cgi?ver=2.0&chv=3")
                                .append("&").append("req_text").append("=").append(reqText)
                                .append("&").append("skey").append("=").append(skey)
                                .append("&").append("skey_type").append("=").append(2)
                                .append("&").append("random").append("=").append(random)
                                .append("&msgno=" + generateNo(selfuin))
                                .toString();

                        //抢
                        Bundle hbResponseBundle = (Bundle) callMethod(requestCaller, "a", globalContext, openLuckyMoneyUrl);
                        String hbResponse = (String) callStaticMethod(findClass(REQUEST_UTIL, walletClassLoader), "a", globalContext, random, new String(hbResponseBundle.getByteArray("data")));

                        //查看领取详情
                        String reqText2 = (String) callStaticMethod(findClass(REQUEST_UTIL, walletClassLoader), "a", globalContext, "https://mqq.tenpay.com/cgi-bin/hongbao/qpay_hb_na_detail.cgi?ver=2.0&chv=3", random,requestUrl.toString());

                        String openLuckyMoneyUrl2 = new StringBuffer("https://mqq.tenpay.com/cgi-bin/hongbao/qpay_hb_na_grap.cgi?ver=2.0&chv=3")
                                .append("&").append("req_text").append("=").append(reqText2)
                                .append("&").append("skey").append("=").append(skey)
                                .append("&").append("skey_type").append("=").append(2)
                                .append("&").append("random").append("=").append(random)
                                .append("&msgno=" + generateNo(selfuin))
                                .toString();
                        //打开领取详情
                        Bundle hbResponseBundle2 = (Bundle) callMethod(requestCaller, "a", globalContext, openLuckyMoneyUrl2);
                        String hbResponse2 = (String) callStaticMethod(findClass(REQUEST_UTIL, walletClassLoader), "a", globalContext, random, new String(hbResponseBundle2.getByteArray("data")) );



                        XposedBridge.log("hbResponse:" + hbResponse2);
                        JSONObject jsonobject4 = new JSONObject(hbResponse);
                        JSONObject jsonobject = new JSONObject(hbResponse2);
                        JSONObject jsonobject2=jsonobject.getJSONObject("send_object");

                        String name = jsonobject2.optString("send_name");
                        String xiangqing="红包详情："+"\n";//红包详情
                        int state = jsonobject4.optInt("state");
                        //没抢到才有效？
                        // try {
                        JSONArray jsonobject3 = jsonobject.optJSONArray("recv_array");
                        double zongjine = ((double) jsonobject2.getInt("total_amount")) / 100.0d;//总金额
                        int baoshu = jsonobject2.getInt("total_num");//总包数
                        int yiqiang_baoshu = jsonobject2.getInt("recv_num");//已抢包数
                        double yiqiang_jine = ((double) jsonobject2.getInt("recv_amount")) / 100.0d;//已抢金额
                        String lucky_wang_temp = jsonobject2.optString("lucky_uin");//运气王
                        Object lucky_wang=null;
                        for (int i = yiqiang_baoshu - 1; i >= 0; i--) {
                            String temp_id=jsonobject3.getJSONObject(i).getString("recv_uin");
                            if(temp_id.equals(lucky_wang_temp) ) {
                                lucky_wang =jsonobject3.getJSONObject(i).getString("recv_name");
                                break;
                            }
                        }

                        String lucky_state=null;//判断抢完未

                        if(yiqiang_baoshu!=baoshu){ lucky_state ="未领完";}
                        else{lucky_state ="运气王：" + lucky_wang;}
                        xiangqing = "关键词：" + password + "\n" + "已抢金额\\总金额：" + yiqiang_jine + "\\" + zongjine + "\n" + "已抢包数\\总包数：" + yiqiang_baoshu + "\\" + baoshu + "\n" + lucky_state;//红包详情
                        for (int i = yiqiang_baoshu - 1; i >= 0; i--) {
                            xiangqing = xiangqing + "\n" + "抢包" + (yiqiang_baoshu - i) + "：" + jsonobject3.getJSONObject(i).getString("recv_name") + "，" + "金额：" + ((double) jsonobject3.getJSONObject(i).getInt("amount")) / 100.0d;
                        }
                        // }
                        // catch(Throwable localThrowable2){}

                        //
                        if (istroop == 1) {
                            from = "昵称：" + name + "\n"+"Q    Q："+senderuin+"\n" + "群名：" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", frienduin), "troopname")+"\n"+"群号："+frienduin;
                        } else if (istroop == 5) {
                            from = "来自:" + name + "\n" + "来自热聊:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(HotChatManager, "a", "com.tencent.mobileqq.data.HotChatInfo", frienduin), "name");
                        } else if (istroop == 3000) {
                            from = "来自:" + name + "\n" + "来自讨论组:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(DiscussionManager, "a", "com.tencent.mobileqq.data.DiscussionInfo", frienduin), "discussionName");
                        } else {
                            from = "来自:" + name;
                        }

                        if (state == 0) {
                            double amount = ((double) jsonobject4.getJSONObject("recv_object").getInt("amount")) / 100.0d;
                            toast("抢到：" + amount + "元" + "\n" + from+"\n"+xiangqing);
                            if (PreferencesUtils.reply() == GOT || PreferencesUtils.reply() == ALL && !TextUtils.isEmpty(PreferencesUtils.gotReply()) && messageType != 8) {
                                callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, PreferencesUtils.gotReply(), new ArrayList(), messageParam);
                            }

                        } else if (state == 2) {

                            if (messageType != 8) {
                                toast("没抢到" + "\n" + from+"\n"+xiangqing);
                                if (PreferencesUtils.reply() == MISSED || PreferencesUtils.reply() == ALL && !TextUtils.isEmpty(PreferencesUtils.missedReply()) && messageType != 8) {
                                    callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, PreferencesUtils.missedReply(), new ArrayList(), messageParam);
                                }
                            }

                        }

                        if (6 == messageType && PreferencesUtils.password() == SEND) {
                            callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, password, new ArrayList(), messageParam);
                        }
                        //发送文本
                        String zhuijia_text=xiangqing +"\n"+"由JJ红包插件免费提供的服务";
                        callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, zhuijia_text, new ArrayList(), messageParam);
                        //
                        //管理群_踢人
                        if(PreferencesUtils.auto_ti()) {
                            String mobao_list = PreferencesUtils.mobao();
                            int flag=0;//踢
                            String tichu_text=null;
                            if (!TextUtils.isEmpty(mobao_list)) {
                                for (int j = yiqiang_baoshu - 1; j >= 0; j--) {
                                    String recv_uin= jsonobject3.getJSONObject(j).getString("recv_uin");
                                    double tou_jine=((double) jsonobject3.getJSONObject(j).getInt("amount")) / 100.0d;
                                    flag=0;

                                    for (String mobao : mobao_list.split(",")) {
                                        if (recv_uin.equals(mobao)) {
                                            flag =1;//不用踢
                                            break;
                                        }
                                        else{flag=0;}
                                    }
                                    if(flag ==0){
                                        //踢出recv_uin

                                        //toast(getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", frienduin), "").toString() ) ;
                                        //msg踢出消息
                                        tichu_text ="发现偷包："+recv_uin+"\n"+"被偷金额："+tou_jine+"\n"+"状态：已踢出";
                                        callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, tichu_text, new ArrayList(), messageParam);

                                    }
                                }
                            }
                        }
                        //
                    }
                }
        );


        findAndHookMethod("com.tencent.mobileqq.app.MessageHandlerUtils", loadPackageParam.classLoader, "a",
                "com.tencent.mobileqq.app.QQAppInterface",
                "com.tencent.mobileqq.data.MessageRecord", Boolean.TYPE, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!PreferencesUtils.open()) {
                            return;
                        }

                        int msgtype = (int) getObjectField(param.args[1], "msgtype");
                        if(msgtype ==-1000) { //根据聊天内容作出反应
                            selfuin = getObjectField(param.args[1], "selfuin").toString();
                            senderuin = (String) getObjectField(param.args[1], "senderuin");
                            if(selfuin.equals(senderuin)) {return ;}
                            frienduin = getObjectField(param.args[1], "frienduin").toString();
                            istroop = (int) getObjectField(param.args[1], "istroop");
                            //修复发送文本不能发送给正确的群的问题
                            SessionInfo = newInstance(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader));
                            findFieldByClassAndTypeAndName(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader), String.class, "a").set(SessionInfo, frienduin);
                            findFieldByClassAndTypeAndName(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader), Integer.TYPE, "a").setInt(SessionInfo, istroop);
                            messageParam = newInstance(findClass("com.tencent.mobileqq.activity.ChatActivityFacade$SendMsgParams", loadPackageParam.classLoader));
                            msgContext = (String) getObjectField(param.args[1], "msg");//读取聊天内容
                            String cha = "查", cha_shuying = "查盈利", hb_record = "排行榜", msg_text = "无记录";//待发送文本，"查"包括流水和输赢，"查盈利"和"排行榜"只有庄家才能查
                            if (msgContext.equals(cha)) {
                                //发送文本

                                msg_text = "红包流水记录如下";
                                callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, msg_text, new ArrayList(), messageParam);
                            }
                            if (msgContext.equals(cha_shuying)) {
                                //发送文本

                                msg_text = "盈利记录如下";
                                callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, msg_text, new ArrayList(), messageParam);
                            }
                            if (msgContext.equals(hb_record)) {
                                //发送文本

                                msg_text = "排行榜记录如下";
                                callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, msg_text, new ArrayList(), messageParam);
                            }

                            //

                        }
                        if (msgtype == -2025) {//发的是红包
                            msgUid = (long) getObjectField(param.args[1], "msgUid");
                            senderuin = (String) getObjectField(param.args[1], "senderuin");
                            frienduin = getObjectField(param.args[1], "frienduin").toString();
                            istroop = (int) getObjectField(param.args[1], "istroop");
                            selfuin = getObjectField(param.args[1], "selfuin").toString();
                        }
                    }
                }

        );

        findAndHookMethod("com.tencent.mobileqq.activity.PublicTransFragmentActivity", loadPackageParam.classLoader, "b",
                Context.class, Intent.class, Class.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("PublicTransFragmentActivity param[0]:" + param.args[0]);
                        XposedBridge.log("PublicTransFragmentActivity param[1]:" + param.args[1]);
                        XposedBridge.log("PublicTransFragmentActivity param[2]:" + param.args[2]);
                    }
                }

        );

        findAndHookMethod("com.tencent.mobileqq.activity.PublicTransFragmentActivity", loadPackageParam.classLoader, "b",
                Activity.class, Intent.class, Class.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("PublicTransFragmentActivity param[0]:" + param.args[0]);
                        XposedBridge.log("PublicTransFragmentActivity param[1]:" + param.args[1]);
                        XposedBridge.log("PublicTransFragmentActivity param[2]:" + param.args[2]);
                        XposedBridge.log("PublicTransFragmentActivity param[3]:" + param.args[3]);
                    }
                }

        );


        findAndHookMethod("com.tencent.mobileqq.activity.SplashActivity", loadPackageParam.classLoader, "doOnCreate", Bundle.class, new

                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        globalContext = (Context) param.thisObject;
                        globalQQInterface = findFirstFieldByExactType(findClass("com.tencent.mobileqq.activity.SplashActivity", loadPackageParam.classLoader), findClass("com.tencent.mobileqq.app.QQAppInterface", loadPackageParam.classLoader)).get(param.thisObject);

                    }
                }

        );


        findAndHookConstructor("mqq.app.TicketManagerImpl", loadPackageParam.classLoader, "mqq.app.AppRuntime", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                TicketManager = param.thisObject;
            }
        });


        findAndHookConstructor("com.tencent.mobileqq.app.HotChatManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new

                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        HotChatManager = param.thisObject;
                    }
                }
        );

        findAndHookConstructor("com.tencent.mobileqq.app.TroopManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                TroopManager = methodHookParam.thisObject;
            }
        });

        findAndHookConstructor("com.tencent.mobileqq.app.DiscussionManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                DiscussionManager = methodHookParam.thisObject;
            }
        });

        findAndHookConstructor("com.tencent.mobileqq.app.FriendsManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                FriendManager = methodHookParam.thisObject;
            }
        });

    }




    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        XposedBridge .log("回调函数") ;
        if (loadPackageParam.packageName.equals(QQ_PACKAGE_NAME)) {
            hideModule(loadPackageParam);

            int ver = Build.VERSION.SDK_INT;
            XposedBridge .log("ver："+ver) ;
            if (ver < 21) {//劫持
                XposedBridge .log("劫持");
                findAndHookMethod("com.tencent.common.app.BaseApplicationImpl", loadPackageParam.classLoader, "onCreate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        dohook(loadPackageParam);
                        XposedBridge .log("开始劫持");
                    }
                });
            } else {
                dohook(loadPackageParam);
            }
        }
    }
    private int getGroupType() throws IllegalAccessException {
        int grouptype = 0;
        if (istroop == 3000) {
            grouptype = 2;

        } else if (istroop == 1) {
            Map map = (Map) findFirstFieldByExactType(HotChatManager.getClass(), Map.class).get(HotChatManager);
            if (map != null & map.containsKey(frienduin)) {
                grouptype = 5;
            } else {
                grouptype = 1;
            }
        } else if (istroop == 0) {
            grouptype = 0;
        } else if (istroop == 1004) {
            grouptype = 4;

        } else if (istroop == 1000) {
            grouptype = 3;

        } else if (istroop == 1001) {
            grouptype = 6;
        }
        return grouptype;
    }

    private String getGroupuin(int messageType) throws InvocationTargetException, IllegalAccessException {
        if (messageType != 6 && messageType != 13) {
            return senderuin;
        }
        if (istroop == 1) {
            return (String) getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", frienduin), "troopcode");
        } else if (istroop == 5) {
            return (String) getObjectField(findResultByMethodNameAndReturnTypeAndParams(HotChatManager, "a", "com.tencent.mobileqq.data.HotChatInfo", frienduin), "troopCode");
        }
        return senderuin;
    }

    private String generateNo(String selfuin) {
        StringBuilder stringBuilder = new StringBuilder(selfuin);
        stringBuilder.append(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        String count = valueOf(n++);
        int length = (28 - stringBuilder.length()) - count.length();
        for (int i = 0; i < length; i++) {
            stringBuilder.append("0");
        }
        stringBuilder.append(count);
        return stringBuilder.toString();
    }

    private void toast(final String content) {
        if (PreferencesUtils.amount()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(globalContext, content, LENGTH_LONG).show();
                }
            });

        }
    }



}
