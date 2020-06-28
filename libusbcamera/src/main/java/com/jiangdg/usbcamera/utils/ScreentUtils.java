package com.jiangdg.usbcamera.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.serenegiant.usb.Size;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by Administrator on 2017/12/18.
 */

public class ScreentUtils {

    private static boolean isPort = false;//是否为竖屏

    public static void init(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getRealMetrics(metrics);
        isPort = metrics.widthPixels < metrics.heightPixels;
    }

    /**
     * 是否为竖屏状态
     *
     * @return
     */
    public static boolean isPort() {
        return isPort;
    }

    /**
     * 检测相机旋转方向是否处于指定拓展模式
     * （当屏幕处于竖屏状态 相机旋转选择90°或者270°）
     *
     * @return
     */
    public static boolean isCameraExtend() {
        String camerarot = ScreentUtils.getValueFromProp("persist.sys.camerarot");
        String displayrot = ScreentUtils.getValueFromProp("persist.sys.displayrot");
        int rotation = Integer.parseInt(displayrot);
        String displayrotcam = ScreentUtils.getValueFromProp("persist.sys.displayrot.cam");
        int realCameraRotation = Integer.parseInt(displayrotcam);
        if (camerarot.equals("0") && isPort && (realCameraRotation
                == 90 || realCameraRotation == 270)) {
            return true;
        }
        return false;
    }

    /**
     * 获取相机旋转角度
     *
     * @return
     */
    public static int getCameraRotation() {
        String camerarot = ScreentUtils.getValueFromProp("persist.sys.camerarot");
        String displayrot = ScreentUtils.getValueFromProp("persist.sys.displayrot");
        int rotation = Integer.parseInt(displayrot);
        if (rotation == 180) rotation = 0;
        if (rotation == 270) rotation = 90;
        if (camerarot.equals("0")) {
            String displayrotcam = ScreentUtils.getValueFromProp("persist.sys.displayrot.cam");
            int realCameraRotation = Integer.parseInt(displayrotcam);
            if (rotation == 90) {
                realCameraRotation = realCameraRotation - 90;
            }
            rotation = realCameraRotation + rotation;
            rotation = rotation % 360;
        } else {
            rotation = 0;
        }
        return rotation;
    }

    /**
     * 后置摄像头是否镜像
     *
     * @return
     */
    public static boolean isBackCameraMirror() {
        String bcamMirror = ScreentUtils.getValueFromProp("persist.hal.bcam.mirror");
        return Boolean.parseBoolean(bcamMirror);
    }

    /**
     * 前置摄像头是否镜像
     *
     * @return
     */
    public static boolean isFrontCameraMirror() {
        String bcamMirror = ScreentUtils.getValueFromProp("persist.hal.fcam.mirror");
        return Boolean.parseBoolean(bcamMirror);
    }

    /**
     * 缓存当前相机宽高
     *
     * @param context
     * @param width
     * @param height
     */
    public static void setCacheResolution(Context context, int width, int height) {
        SharedPreferences sp = context.getSharedPreferences("cache_rlt", Context.MODE_PRIVATE);
        sp.edit().putString("_rlt", width + "x" + height).commit();
    }

    /**
     * 获取缓存相机宽高
     *
     * @param context
     * @param sizeList
     * @return int[]{width，height}；
     */
    public static Size getCacheResolution(Context context, List<Size> sizeList) {
        SharedPreferences sp = context.getSharedPreferences("cache_rlt", Context.MODE_PRIVATE);
        String rlt = sp.getString("_rlt", "");
        Size maxSize = getMaxSupportedPreviewSize(sizeList);
        if (TextUtils.isEmpty(rlt) || maxSize == null) return maxSize;
        String[] xes = rlt.split("x");
        int width = Integer.parseInt(xes[0]);
        int height = Integer.parseInt(xes[1]);
        boolean rotaExits = false;
        for (Size sz : sizeList) {
            if (sz.width == width && sz.height == height) {
                rotaExits = true;
                break;
            }
        }
        if (rotaExits)
            return new Size(0, 0, 0, width, height);
        else
            return maxSize;
    }

    private static Size getMaxSupportedPreviewSize(List<Size> sp) {
        if (sp == null || sp.size() <= 0) return null;
        long maxSize = sp.get(0).width * sp.get(0).height;
        Size max = sp.get(0);
        for (int i = 0; i < sp.size(); i++) {
            long curSize = sp.get(i).width * sp.get(i).height;
            if (curSize > maxSize) {
                maxSize = curSize;
                max = sp.get(i);
            }
        }
        return max;
    }

    public static String execCommand(String command) {
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        Runtime runtime;
        Process proc = null;
        StringBuffer stringBuffer = new StringBuffer();
        try {
            runtime = Runtime.getRuntime();
            proc = runtime.exec(command);
            if (proc.waitFor() != 0) {
                System.err.println("exit value = " + proc.exitValue());
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                stringBuffer.append(line + " ");
            }
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            try {
                proc.destroy();
            } catch (Exception e2) {
            }
        }
        return stringBuffer.toString();
    }

    public static String execCommandSu(String command) {
        Process process = null;
        DataOutputStream os = null;
        StringBuffer stringBuffer = new StringBuffer();
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            if (process.waitFor() != 0) {
                System.err.println("exit value = " + process.exitValue());
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                stringBuffer.append(line + " ");
            }
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return stringBuffer.toString();
    }

    public static String getValueFromProp(String key) {
        String value = "";
        try {
            Class<?> classType = Class.forName("android.os.SystemProperties");
            Method getMethod = classType.getDeclaredMethod("get", new Class<?>[]{String.class});
            value = (String) getMethod.invoke(classType, new Object[]{key});
        } catch (Exception e) {
        }
        return value;
    }

    public static void setValueToProp(String key, String val) {
        Class<?> classType;
        try {
            classType = Class.forName("android.os.SystemProperties");
            Method method = classType.getDeclaredMethod("set", new Class[]{String.class, String.class});
            method.invoke(classType, new Object[]{key, val});
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
