package com.example.surfaceview;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private final String TAG = "JFQ_LOG";
    private Camera camera;
    private int width;
    private int height;
    private SurfaceView surfaceView;
    private LinearLayout linearLayout;
    private Camera.Size previewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initCamera();
        initSurfaceView();
    }

    private void initView() {
        Display display = getWindowManager().getDefaultDisplay();
        width = display.getWidth();
        height = display.getHeight();
        Log.d(TAG, "onCreate: getDisplayMetrics:" + width + "x" + height);
        surfaceView = findViewById(R.id.surface_view);
        linearLayout = findViewById(R.id.linear_layout);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(linearLayout.getLayoutParams());
        layoutParams.width = width;
        layoutParams.height = height;
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.invalidate();
    }

    private void initSurfaceView() {
        surfaceView.getHolder().addCallback(this);
        Matrix matrix = calculateSurfaceHolderTransform(previewSize);
        float[] values = new float[9];
        matrix.getValues(values);
        linearLayout.setTranslationX(values[Matrix.MTRANS_X]);
        linearLayout.setTranslationY(values[Matrix.MTRANS_Y]);
        linearLayout.setScaleX(values[Matrix.MSCALE_X]);
        linearLayout.setScaleY(values[Matrix.MSCALE_Y]);
        linearLayout.invalidate();
    }

    public Matrix calculateSurfaceHolderTransform(Camera.Size previewSize) {
        // 预览 View 的大小
        int viewHeight = width;
        int viewWidth = height;
        // 相机选择的预览尺寸
        int cameraHeight = previewSize.height;
        int cameraWidth = previewSize.width;
        // 计算出将相机的尺寸 => View 的尺寸 需要的缩放倍数
        float ratioPreview = (float) cameraWidth / cameraHeight;
        float ratioView = (float) viewWidth / viewHeight;
        float scaleX, scaleY;
        if (ratioView < ratioPreview) {
            scaleX = ratioPreview / ratioView;
            scaleY = 1;
        } else {
            scaleX = 1;
            scaleY = ratioView / ratioPreview;
        }
        // 计算出View偏移量
        float scaledWidth = viewWidth * scaleX;
        float scaledHeight = viewHeight * scaleY;
        float dx = (viewWidth - scaledWidth) / 2;
        float dy = (viewHeight - scaledHeight) / 2;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);
        matrix.postTranslate(dx, dy);
        return matrix;
    }

    private void initCamera() {
        int frontId = findFrontCamera();
        camera = Camera.open(frontId);
        camera.setDisplayOrientation(90);
        Camera.Parameters params = camera.getParameters();
        previewSize = getPropPreviewSize(params.getSupportedPreviewSizes());
        params.setPreviewSize(previewSize.width, previewSize.height);
        camera.setParameters(params);
    }

    private int findFrontCamera() {
        int cameraCount;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (camera != null) {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public Camera.Size getPropPreviewSize(List<Camera.Size> sizes) {
        final double ASPECT_TOLERANCE = 0.1;
        // 竖屏是 h/w, 横屏是 w/h
        double targetRatio = (double) height / width;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = height;
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
}