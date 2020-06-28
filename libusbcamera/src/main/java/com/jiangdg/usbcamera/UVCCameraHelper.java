package com.jiangdg.usbcamera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.jiangdg.libusbcamera.R;
import com.jiangdg.usbcamera.utils.ScreentUtils;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.common.UVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * UVCCamera Helper class
 * <p>
 * Created by jiangdongguo on 2017/9/30.
 */

public class UVCCameraHelper {
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator;
    public static final String SUFFIX_JPEG = ".jpg";
    public static final String SUFFIX_MP4 = ".mp4";
    private static final String TAG = "UVCCameraHelper";
    private int previewWidth = 640;
    private int previewHeight = 480;
    private int mDegree = 0;
    private boolean isMirror = true;
    public static final int FRAME_FORMAT_YUYV = UVCCamera.FRAME_FORMAT_YUYV;
    // Default using MJPEG
    // if your device is connected,but have no images
    // please try to change it to FRAME_FORMAT_YUYV
    public static final int FRAME_FORMAT_MJPEG = UVCCamera.FRAME_FORMAT_MJPEG;
    public static final int MODE_BRIGHTNESS = UVCCamera.PU_BRIGHTNESS;
    public static final int MODE_CONTRAST = UVCCamera.PU_CONTRAST;
    private int mFrameFormat = FRAME_FORMAT_MJPEG;

    private static UVCCameraHelper mCameraHelper;
    // USB Manager
    private USBMonitor mUSBMonitor;
    // Camera Handler
    private UVCCameraHandler mCameraHandler;
    private USBMonitor.UsbControlBlock mCtrlBlock;

    private Activity mActivity;
    private CameraViewInterface mCamView;
    private int cameraIndex = 0;

    private UVCCameraHelper() {
    }

    public static UVCCameraHelper newInstance() {
        return new UVCCameraHelper();
    }

    public static UVCCameraHelper getInstance() {
        if (mCameraHelper == null) {
            mCameraHelper = new UVCCameraHelper();
        }
        return mCameraHelper;
    }

    public void closeCamera() {
        if (mCameraHandler != null) {
            mCameraHandler.close();
        }
    }

    public void addCallback(AbstractUVCCameraHandler.CameraCallback callback) {
        if (mCameraHandler != null) {
            mCameraHandler.addCallback(callback);
        }
    }

    public interface OnMyDevConnectListener {
        void onAttachDev(UsbDevice device);

        void onDettachDev(UsbDevice device);

        void onConnectDev(UsbDevice device, boolean isConnected);

        void onDisConnectDev(UsbDevice device);
    }

    public void initUSBMonitor(Activity activity, CameraViewInterface cameraView, final OnMyDevConnectListener listener) {
        this.mActivity = activity;
        this.mCamView = cameraView;
        ScreentUtils.init(activity);
        mUSBMonitor = new USBMonitor(activity.getApplicationContext(), new USBMonitor.OnDeviceConnectListener() {

            // called by checking usb device
            // do request device permission
            @Override
            public void onAttach(UsbDevice device) {
                if (listener != null) {
                    listener.onAttachDev(device);
                }
            }

            // called by taking out usb device
            // do close camera
            @Override
            public void onDettach(UsbDevice device) {
                if (listener != null) {
                    listener.onDettachDev(device);
                }
            }

            // called by connect to usb camera
            // do open camera,start previewing
            @Override
            public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                mCtrlBlock = ctrlBlock;
                openCamera(ctrlBlock);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((UVCCameraTextureView) mCamView).setVisibility(View.INVISIBLE);
                    }
                });
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // wait for camera created
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // start previewing
                        startPreview(mCamView);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((UVCCameraTextureView) mCamView).setVisibility(View.VISIBLE);
                                List<Size> sizeList = getSupportedPreviewSizes();
                                Size size = ScreentUtils.getCacheResolution(mActivity, sizeList);
                                if (size != null) {
                                    updateResolution(size.width, size.height);
                                }
                            }
                        });
                    }
                }).start();
                if (listener != null) {
                    listener.onConnectDev(device, true);
                }
            }

            // called by disconnect to usb camera
            // do nothing
            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                if (listener != null) {
                    listener.onDisConnectDev(device);
                }
            }

            @Override
            public void onCancel(UsbDevice device) {
            }
        });

        createUVCCamera();
    }

    public void setCameraRotation(int rotation) {
//        if (rotation == mRotation) {
//            return;
//        }
//        if (mCameraHandler != null) {
//            mCameraHandler.release();
//            mCameraHandler = null;
//        }
//        this.previewWidth = width;
//        this.previewHeight = height;
//        mCamView.setAspectRatio(previewWidth / (float) previewHeight);
//        mCameraHandler = UVCCameraHandler.createHandler(mActivity, mCamView, 2,
//                previewWidth, previewHeight, mFrameFormat);
//        openCamera(mCtrlBlock);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                // wait for camera created
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                // start previewing
//                startPreview(mCamView);
//            }
//        }).start();
    }

    public void createUVCCamera() {
        if (mCamView == null)
            throw new NullPointerException("CameraViewInterface cannot be null!");

        // release resources for initializing camera handler
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        // initialize camera handler
        mCamView.setAspectRatio(previewWidth / (float) previewHeight);
        if (cameraIndex == 0) {
            isMirror = ScreentUtils.isBackCameraMirror();
        } else {
            isMirror = ScreentUtils.isFrontCameraMirror();
        }
        mDegree = ScreentUtils.getCameraRotation();
        ((UVCCameraTextureView) mCamView).setRotation(mDegree);
        mCamView.setMirror(isMirror);
        mCameraHandler = UVCCameraHandler.createHandler(mActivity, mCamView, 2,
                previewWidth, previewHeight, mDegree, isMirror, mFrameFormat);
    }

    public void setMirror(boolean isMirror) {
//        updateConfig(previewWidth, previewHeight, mDegree, isMirror);
    }

    public void setDegree(int degree) {
//        if (ScreentUtils.isPort()) {
//            if (degree == 0) degree = 90;
//            else if (degree == 90) degree = 180;
//            else if (degree == 180) degree = 270;
//            else if (degree == 270) degree = 0;
//        }
//        updateConfig(previewWidth, previewHeight, degree, isMirror);
    }

    public void updateResolution(int width, int height) {
        updateConfig(width, height, mDegree, isMirror);
        ScreentUtils.setCacheResolution(mActivity, width, height);
    }

    public void updateConfig(int width, int height, int degree, boolean isMirror) {
        if (previewWidth == width && previewHeight == height
                && mDegree == degree && isMirror == this.isMirror) {
            return;
        }
        mDegree = degree;
        this.isMirror = isMirror;
        this.previewWidth = width;
        this.previewHeight = height;
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        mCamView.setMirror(isMirror);
        mCamView.setDegree(degree);
        mCamView.setAspectRatio(previewWidth / (float) previewHeight);
        mCameraHandler = UVCCameraHandler.createHandler(mActivity, mCamView, 2,
                previewWidth, previewHeight, mDegree, isMirror, mFrameFormat);
        openCamera(mCtrlBlock);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // wait for camera created
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // start previewing
                startPreview(mCamView);
            }
        }).start();
    }

    public void registerUSB() {
        if (mUSBMonitor != null) {
            mUSBMonitor.register();
        }
    }

    public void unregisterUSB() {
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
        }
    }

    public boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    public int getModelValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    public int setModelValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    public int resetModelValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }

    public void requestPermission(int index) {
        List<UsbDevice> devList = getUsbDeviceList();
        if (devList == null || devList.size() == 0) {
            return;
        }
        int count = devList.size();
        if (index >= count)
            new IllegalArgumentException("index illegal,should be < devList.size()");
        if (mUSBMonitor != null) {
            cameraIndex = index;
            mUSBMonitor.requestPermission(getUsbDeviceList().get(index));
        }
    }

    public int getUsbDeviceCount() {
        List<UsbDevice> devList = getUsbDeviceList();
        if (devList == null || devList.size() == 0) {
            return 0;
        }
        return devList.size();
    }

    public List<UsbDevice> getUsbDeviceList() {
        List<DeviceFilter> deviceFilters = DeviceFilter
                .getDeviceFilters(mActivity.getApplicationContext(), R.xml.device_filter);
        if (mUSBMonitor == null || deviceFilters == null)
//            throw new NullPointerException("mUSBMonitor ="+mUSBMonitor+"deviceFilters=;"+deviceFilters);
            return null;
        // matching all of filter devices
        return mUSBMonitor.getDeviceList(deviceFilters);
    }

    public void capturePicture(String savePath, AbstractUVCCameraHandler.OnCaptureListener listener) {
        if (mCameraHandler != null && mCameraHandler.isOpened()) {
            mCameraHandler.captureStill(savePath, listener);
        }
    }

    public void startPusher(AbstractUVCCameraHandler.OnEncodeResultListener listener) {
        if (mCameraHandler != null && !isPushing()) {
            mCameraHandler.startRecording(null, listener);
        }
    }

    public void startPusher(RecordParams params, AbstractUVCCameraHandler.OnEncodeResultListener listener) {
        if (mCameraHandler != null && !isPushing()) {
            mCameraHandler.startRecording(params, listener);
        }
    }

    public void stopPusher() {
        if (mCameraHandler != null && isPushing()) {
            mCameraHandler.stopRecording();
        }
    }

    public boolean isPushing() {
        if (mCameraHandler != null) {
            return mCameraHandler.isRecording();
        }
        return false;
    }

    public boolean isCameraOpened() {
        if (mCameraHandler != null) {
            return mCameraHandler.isOpened();
        }
        return false;
    }

    public void release() {
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
    }

    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    public void setOnPreviewFrameListener(AbstractUVCCameraHandler.OnPreViewResultListener listener) {
        if (mCameraHandler != null) {
            mCameraHandler.setOnPreViewResultListener(listener);
        }
    }

    private void openCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        if (mCameraHandler != null) {
            mCameraHandler.open(ctrlBlock);
        }
    }

    public void startPreview(CameraViewInterface cameraView) {
        SurfaceTexture st = cameraView.getSurfaceTexture();
        if (mCameraHandler != null) {
            mCameraHandler.startPreview(st);
        }
    }

    public void stopPreview() {
        if (mCameraHandler != null) {
            mCameraHandler.stopPreview();
        }
    }

    public void startCameraFoucs() {
        if (mCameraHandler != null) {
            mCameraHandler.startCameraFoucs();
        }
    }

    public List<Size> getSupportedPreviewSizes() {
        if (mCameraHandler == null)
            return null;
        return mCameraHandler.getSupportedPreviewSizes();
    }

    public void setDefaultPreviewSize(int defaultWidth, int defaultHeight) {
        if (mUSBMonitor != null) {
            throw new IllegalStateException("setDefaultPreviewSize should be call before initMonitor");
        }
        this.previewWidth = defaultWidth;
        this.previewHeight = defaultHeight;
    }

    public void setDefaultFrameFormat(int format) {
        if (mUSBMonitor != null) {
            throw new IllegalStateException("setDefaultFrameFormat should be call before initMonitor");
        }
        this.mFrameFormat = format;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }
}
