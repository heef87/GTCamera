package com.jiangdg.usbcamera.utils;

import android.os.Environment;

import com.jiangdg.usbcamera.UVCCameraHelper;

import java.io.File;
import java.io.IOException;

public class CameraStoreger {
    static final String DIRECTORY_NAME = "GTCamera";
    static final String DIRECTORY_PICTURE_NAME = "images";
    static final String DIRECTORY_VIDEO_NAME = "videos";

    public static String getPicturePath() {
        File dir = new File(UVCCameraHelper.ROOT_PATH + DIRECTORY_NAME);
        if (!dir.exists()) dir.mkdir();
        File pdir = new File(dir, DIRECTORY_PICTURE_NAME);
        if (!pdir.exists()) pdir.mkdir();
        return pdir.getAbsolutePath();
    }

    public static String getVideoPath() {
        File dir = new File(UVCCameraHelper.ROOT_PATH + DIRECTORY_NAME);
        if (!dir.exists()) dir.mkdir();
        File pdir = new File(dir, DIRECTORY_VIDEO_NAME);
        if (!pdir.exists()) pdir.mkdir();
        return pdir.getAbsolutePath();
    }

    public static File getFailedCollect(){
        File dir = new File(UVCCameraHelper.ROOT_PATH + DIRECTORY_NAME);
        if (!dir.exists()) dir.mkdir();
        File file = new File(dir,  "failed_devices.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }
}
