package com.komachan.nobar;

import android.graphics.Rect;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[NoBar] ";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        try {
            if ("android".equals(lp.packageName)) {
                hookInsets(lp.classLoader);
            } else if ("com.android.systemui".equals(lp.packageName)) {
                hookHandle(lp.classLoader);
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + "load failed: " + t);
        }
    }

    /**
     * system_server: 把 mandatorySystemGestures 的高度压成 0。
     * 这样 app 收到的 bottom inset 变成 0，内容可以铺到底，
     * 但系统手势识别仍然基于触摸坐标，不受影响。
     */
    private void hookInsets(ClassLoader cl) {
        try {
            Class<?> provider = XposedHelpers.findClass(
                    "com.android.server.wm.InsetsSourceProvider", cl);
            Class<?> typeCls = XposedHelpers.findClass(
                    "android.view.WindowInsets$Type", cl);
            final int mandatory = (int) XposedHelpers.callStaticMethod(
                    typeCls, "mandatorySystemGestures");

            XposedBridge.hookAllMethods(provider, "updateSourceFrame",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) {
                            try {
                                Object src = XposedHelpers.getObjectField(
                                        p.thisObject, "mSource");
                                if (src == null) return;
                                int type = (int) XposedHelpers.callMethod(src, "getType");
                                if (type != mandatory) return;
                                Rect r = (Rect) p.args[0];
                                if (r != null && r.bottom > r.top) {
                                    r.bottom = r.top;
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });
            XposedBridge.log(TAG + "inset hook installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "inset hook failed: " + t);
        }
    }

    /**
     * SystemUI: 不绘制底部手势条。
     */
    private void hookHandle(ClassLoader cl) {
        try {
            Class<?> handle = XposedHelpers.findClass(
                    "com.android.systemui.navigationbar.gestural.NavigationHandle", cl);
            XposedBridge.hookAllMethods(handle, "draw", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    p.setResult(null);
                }
            });
            XposedBridge.log(TAG + "handle hook installed");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "handle hook failed: " + t);
        }
    }
}
