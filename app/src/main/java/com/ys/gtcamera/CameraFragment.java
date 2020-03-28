package com.ys.gtcamera;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;

public class CameraFragment extends Fragment implements RadioGroup.OnCheckedChangeListener, View.OnClickListener, AbstractUVCCameraHandler.CameraCallback {
    static final int FLAG_CAMERA_MODE_LAYOUT = 1001;
    private View mTakeLayout;
    private RadioGroup mRadioGroup;
    private ImageButton mOptionButton;
    private ImageButton mSwitchButton;
    private ImageButton mActionButton;
    private ImageButton mSceneButton;
    private UVCCameraTextureView mTextureView;
    private Chronometer mChronometer;
    private AlertDialog mDialog;
    private boolean isCapture = true;
    private MyCamera mMyCamera;
    private CameraViewInterface viewInterface;
    private int index = 0;
    private int count = 0;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            if (message.what == FLAG_CAMERA_MODE_LAYOUT && mRadioGroup != null) {
                aninationCameramode(false);
                mRadioGroup.setVisibility(View.GONE);
            }
            return false;
        }
    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_camera, container, false);
        mTakeLayout = inflate.findViewById(R.id.camera_take_layout);
        mChronometer = inflate.findViewById(R.id.time_chronometer);
        mRadioGroup = inflate.findViewById(R.id.camera_mode_select);
        mRadioGroup.setVisibility(View.GONE);
        mOptionButton = inflate.findViewById(R.id.camera_options);
        mSwitchButton = inflate.findViewById(R.id.camera_switch);
        mActionButton = inflate.findViewById(R.id.camera_cation);
        mSceneButton = inflate.findViewById(R.id.camera_scence);
        mTextureView = inflate.findViewById(R.id.camera_view);
        viewInterface = mTextureView;
        mRadioGroup.setOnCheckedChangeListener(this);
        mOptionButton.setOnClickListener(this);
        mSwitchButton.setOnClickListener(this);
        mActionButton.setOnClickListener(this);
        mSceneButton.setOnClickListener(this);
        inflate.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    float dx = motionEvent.getX();
                    if (dx <= 20) {
                        mRadioGroup.setVisibility(View.VISIBLE);
                        aninationCameramode(true);
                        mHandler.sendEmptyMessageDelayed(FLAG_CAMERA_MODE_LAYOUT, 2000);
                    }
                }
                return false;
            }
        });
        return inflate;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMyCamera = new MyCamera(getActivity(), viewInterface, index);
        mMyCamera.setCameraCallback(this);
        count = mMyCamera.getCount();
        setSwitchVisibility(count > 1);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMyCamera != null) {
            mMyCamera.register();
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        if (mMyCamera != null) {
            mMyCamera.unregister();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTakeLayout = null;
        mHandler.removeMessages(FLAG_CAMERA_MODE_LAYOUT);
        mChronometer.stop();
        mChronometer = null;
        if (mMyCamera != null) {
            mMyCamera.release();
        }
        mMyCamera = null;
    }

    @Override
    public void onClick(View view) {
        if (view == mOptionButton) {
            showResolutionListDialog();
        } else if (view == mSwitchButton) {
            switchCamare();
        } else if (view == mActionButton) {
            if (isCapture) {
                mMyCamera.takePicture();
                AlphaAnimation alphaAnimation = new AlphaAnimation(0.5f,1f);
                alphaAnimation.setDuration(600);
                alphaAnimation.setFillAfter(true);
                mTextureView.startAnimation(alphaAnimation);
            } else {
                if (mMyCamera.isRecording()) {
                    mMyCamera.stopRecord();
                    mChronometer.stop();
                    mActionButton.setImageResource(R.mipmap.btn_shutter_video_default);
                    cameraRecordStatus(false);
                } else {
                    mMyCamera.starRecord();
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    mChronometer.setVisibility(View.VISIBLE);
                    mActionButton.setImageResource(R.mipmap.btn_shutter_video_recording);
                    cameraRecordStatus(true);
                }
            }
        } else if (view == mSceneButton) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("image/*");
            getActivity().startActivity(intent);
        }
    }

    private void cameraRecordStatus(boolean isRecording) {
        setSwitchVisibility(!isRecording && (count > 1));
        if (isRecording) {
            mOptionButton.setVisibility(View.INVISIBLE);
            mSceneButton.setVisibility(View.INVISIBLE);
        } else {
            mOptionButton.setVisibility(View.VISIBLE);
            mSceneButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        cameraRecordStatus(false);
        if (i == R.id.camera_capture) {
            mMyCamera.stopRecord();
            isCapture = true;
            mChronometer.stop();
            mChronometer.setVisibility(View.INVISIBLE);
            mActionButton.setImageResource(R.drawable.ic_capture_camera);
        } else if (i == R.id.camera_video) {
            isCapture = false;
            mActionButton.setImageResource(R.mipmap.btn_shutter_video_default);
        }
    }

    private void aninationCameramode(boolean isShow) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(isShow ? 0 : 1, isShow ? 1 : 0);
        TranslateAnimation mHiddenAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                isShow ? -1.0f : 0.0f, Animation.RELATIVE_TO_SELF, isShow ? 0.0f : -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(alphaAnimation);
        animationSet.addAnimation(mHiddenAction);
        animationSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animationSet.setFillAfter(true);
        animationSet.setDuration(600);
        mHiddenAction.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mRadioGroup.clearAnimation();
                mRadioGroup.invalidate();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mRadioGroup.setAnimation(animationSet);

    }

    private void showResolutionListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View rootView = LayoutInflater.from(getContext()).inflate(R.layout.layout_dialog_list, null);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_dialog);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, mMyCamera.getResolutionList());
        if (adapter != null) {
            listView.setAdapter(adapter);
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (!mMyCamera.isCameraOpened())
                    return;
                final String resolution = (String) adapterView.getItemAtPosition(position);
                String[] tmp = resolution.split("x");
                if (tmp != null && tmp.length >= 2) {
                    int widht = Integer.valueOf(tmp[0]);
                    int height = Integer.valueOf(tmp[1]);
                    mMyCamera.updateResolution(widht, height);
                }
                mDialog.dismiss();
            }
        });

        builder.setView(rootView);
        mDialog = builder.create();
        mDialog.show();
    }


    private void switchCamare() {
        index++;
        if (index >= count) {
            index = 0;
        }
        mMyCamera.unregister();
        mMyCamera.release();
        mMyCamera = null;
        mMyCamera = new MyCamera(getActivity(), viewInterface, index);
//        mMyCamera.setCameraCallback(this);
        mMyCamera.register();
    }

    public void setSwitchVisibility(boolean isV) {
        if (isV) {
            mSwitchButton.setVisibility(View.VISIBLE);
        } else {
            mSwitchButton.setVisibility(View.INVISIBLE);
        }
    }

    public boolean isRecording() {
        if (mMyCamera != null) {
            return mMyCamera.isRecording();
        }
        return false;
    }

    @Override
    public void onOpen() {

    }

    @Override
    public void onClose() {

    }

    @Override
    public void onStartPreview() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mTakeLayout != null)
                    mTakeLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onStopPreview() {

    }

    @Override
    public void onStartRecording() {

    }

    @Override
    public void onStopRecording() {

    }

    @Override
    public void onError(Exception e) {

    }
}
