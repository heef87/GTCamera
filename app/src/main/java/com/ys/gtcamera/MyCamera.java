package com.ys.gtcamera;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Surface;

import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.utils.CameraStoreger;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MyCamera implements CameraViewInterface.Callback {
    private static final String TAG = "MyCamera";
    private int cameraIndex = 0;
    private UVCCameraHelper mCameraHelper;
    private CameraViewInterface viewInterface;
    private boolean isRequest;
    private boolean isPreview;
    private Activity mActivity;
    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(cameraIndex);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                isPreview = false;
            } else {
                isPreview = true;
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {

        }
    };

    public MyCamera(Activity activity, CameraViewInterface viewInterface, int cameraIndex) {
        this.viewInterface = viewInterface;
        this.cameraIndex = cameraIndex;
        mActivity = activity;
        viewInterface.setCallback(this);
        mCameraHelper = UVCCameraHelper.newInstance();
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
        mCameraHelper.initUSBMonitor(activity, viewInterface, listener);
    }

    public void setCameraCallback(AbstractUVCCameraHandler.CameraCallback callback) {
        mCameraHelper.addCallback(callback);
    }

    public void setOnPreviewFrameListener(AbstractUVCCameraHandler.OnPreViewResultListener listener) {
        mCameraHelper.setOnPreviewFrameListener(listener);
    }

    public int getCount() {
        return mCameraHelper.getUsbDeviceCount();
    }

    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        if (!isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.startPreview(viewInterface);
            isPreview = true;
        }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (mCameraHelper == null) return;
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
    }

    public void register() {
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    public void unregister() {
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }

    public void release() {
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
    }

    public List<String> getResolutionList() {
        List<Size> list = mCameraHelper.getSupportedPreviewSizes();
        List<String> resolutions = null;
        if (list != null && list.size() != 0) {
            resolutions = new ArrayList<>();
            for (Size size : list) {
                if (size != null) {
                    resolutions.add(size.width + "x" + size.height);
                }
            }
        }
        return resolutions;
    }

    public boolean isCameraOpened() {
        return mCameraHelper != null && mCameraHelper.isCameraOpened();
    }

    public void updateResolution(int widht, int height) {
        mCameraHelper.updateResolution(widht, height);
    }

    public void takePicture() {
        if (!isCameraOpened()) {
            return;
        }
        final String filaName = System.currentTimeMillis() + UVCCameraHelper.SUFFIX_JPEG;
        mCameraHelper.capturePicture(CameraStoreger.getPicturePath() + File.separator
                        + filaName,
                new AbstractUVCCameraHandler.OnCaptureListener() {
                    @Override
                    public void onCaptureResult(String path) {
                        mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path))));
                    }
                });
    }

    public boolean isRecording() {
        return mCameraHelper.isPushing();
    }

    public void starRecord() {
        if (!isCameraOpened()) {
            return;
        }
        if (!isRecording()) {
            RecordParams params = new RecordParams();
            params.setRecordPath(CameraStoreger.getVideoPath() + File.separator + System.currentTimeMillis());
            params.setRecordDuration(0);
            params.setVoiceClose(true);
            mCameraHelper.startPusher(params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                @Override
                public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                }

                @Override
                public void onRecordResult(String videoPath) {
                    mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(videoPath))));
                }
            });
        }
    }

    public void stopRecord() {
        if (!isCameraOpened()) {
            return;
        }
        if (isRecording())
            mCameraHelper.stopPusher();
    }

}
