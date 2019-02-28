/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android.view;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private final Paint paint;
    /**
     * 遮罩层颜色
     */
    private final int maskColor;
    /**
     * 扫描线风格:0x0-paint,0x1-bitmap
     */
    private final int laserStyle;
    /**
     * 扫描线颜色
     */
    private final int laserColor;
    /**
     * 结果点颜色
     */
    private final int resultPointColor;
    /**
     * 闪烁点List
     */
    private final List<ResultPoint> possibleResultPoints;
    /**
     * 上次闪烁点List
     */
    private final List<ResultPoint> lastPossibleResultPoints;
    /**
     * 四个绿色边角对应的长度
     */
    private final int cornerLength;
    /**
     * 四个绿色边角对应的厚度
     */
    private final int cornerThickness;
    /**
     * 提示文字字体大小
     */
    private final int tipTextSize;
    /**
     * 提示文字距离扫描框下面的高度
     */
    private final int tipTextMarginTop;
    /**
     * 刷新界面的时间
     */
    private long animationDelay;
    /**
     * 闪烁点透明度
     */
    private int currentPointOpacity;
    /**
     * 闪烁点数目
     */
    private int possiblePointNum;
    /**
     * 闪烁点最大数目
     */
    private int maxResultPoints;
    /**
     * 闪烁点半径
     */
    private int possiblePointSize;
    private CameraManager cameraManager;
    /**
     * 扫描线样式图片
     */
    private Bitmap laserBitmap;
    /**
     * 扫描线不透明度
     */
    private int scannerAlpha;
    /**
     * 中间扫描线每次刷新移动的距离
     */
    private int scanningDistance;
    /**
     * 中间扫描线的最顶端位置
     */
    private int slideTop;
    /**
     * 中间扫描线高度
     */
    private int laserHeight;
    /**
     * 提示文字
     */
    private String tipText;
    /**
     * 提示文字透明度
     */
    private int tipTextAlpha;
    /**
     * 提示文字颜色
     */
    private int tipTextColor;
    /**
     * 是否为第一次Draw
     */
    private boolean isFirstDraw = true;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView);
        animationDelay = a.getInt(R.styleable.ViewfinderView_animationDelay,
                resources.getInteger(R.integer.animation_delay));
        currentPointOpacity = a.getInt(R.styleable.ViewfinderView_currentPointOpacity,
                resources.getInteger(R.integer.current_point_opacity));
        possiblePointNum = a.getInt(R.styleable.ViewfinderView_possiblePointNum,
                resources.getInteger(R.integer.possible_point_num));
        maxResultPoints = a.getInt(R.styleable.ViewfinderView_maxResultPoints,
                resources.getInteger(R.integer.max_result_points));
        possiblePointSize = (int) a.getDimension(R.styleable.ViewfinderView_possiblePointSize,
                resources.getDimension(R.dimen.possible_point_size));

        maskColor = a.getColor(R.styleable.ViewfinderView_maskColor,
                resources.getColor(R.color.viewfinder_mask));
        laserStyle = a.getInt(R.styleable.ViewfinderView_laserStyle,
                resources.getInteger(R.integer.laser_style));
        if (0x1 == laserStyle) {
            BitmapDrawable bmpDrawable = (BitmapDrawable)
                    context.getResources().getDrawable(R.drawable.laser_line);
            laserBitmap = bmpDrawable.getBitmap();
        }
        laserColor = a.getColor(R.styleable.ViewfinderView_laserColor,
                resources.getColor(R.color.viewfinder_laser));
        resultPointColor = a.getColor(R.styleable.ViewfinderView_resultPointColor,
                resources.getColor(R.color.possible_result_points));
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(possiblePointNum);
        lastPossibleResultPoints = new ArrayList<>(possiblePointNum);

        cornerLength = (int) a.getDimension(R.styleable.ViewfinderView_cornerLength,
                resources.getDimension(R.dimen.corner_length));
        cornerThickness = (int) a.getDimension(R.styleable.ViewfinderView_cornerThickness,
                resources.getDimension(R.dimen.corner_thickness));
        scanningDistance = (int) a.getDimension(R.styleable.ViewfinderView_scanningDistance,
                resources.getDimension(R.dimen.scanning_distance));
        laserHeight = (int) a.getDimension(R.styleable.ViewfinderView_laserHeight,
                resources.getDimension(R.dimen.laser_height));

        tipText = a.getString(R.styleable.ViewfinderView_tipText);
        if (TextUtils.isEmpty(tipText)) {
            tipText = resources.getString(R.string.msg_default_status);
        }
        tipTextAlpha = a.getInt(R.styleable.ViewfinderView_tipTextAlpha,
                resources.getInteger(R.integer.tip_text_alpha));
        tipTextColor = a.getColor(R.styleable.ViewfinderView_tipTextColor,
                resources.getColor(R.color.status_text));
        tipTextSize = (int) a.getDimension(R.styleable.ViewfinderView_tipTextSize,
                resources.getDimension(R.dimen.tip_text_size));
        tipTextMarginTop = dp2px(context, a.getDimensionPixelSize(R.styleable.ViewfinderView_tipTextMarginTop,
                resources.getDimensionPixelSize(R.dimen.tip_text_margin_top)));
        a.recycle();
    }

    private int dp2px(Context context, int dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue,
                context.getResources().getDisplayMetrics());
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = getWidth();
        int height = getHeight();

        // 初始化中间线滑动的最上边和最下边
        if (isFirstDraw) {
            isFirstDraw = false;
            slideTop = frame.top;
        }

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        // 画扫描框边上的角，总共8个部分
        paint.setColor(Color.GREEN);
        canvas.drawRect(frame.left, frame.top, frame.left + cornerLength,
                frame.top + cornerThickness, paint);
        canvas.drawRect(frame.left, frame.top, frame.left
                + cornerThickness, frame.top + cornerLength, paint);
        canvas.drawRect(frame.right - cornerLength, frame.top,
                frame.right, frame.top + cornerThickness, paint);
        canvas.drawRect(frame.right - cornerThickness, frame.top,
                frame.right, frame.top + cornerLength, paint);
        canvas.drawRect(frame.left, frame.bottom - cornerThickness,
                frame.left + cornerLength, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - cornerLength,
                frame.left + cornerThickness, frame.bottom, paint);
        canvas.drawRect(frame.right - cornerLength, frame.bottom
                - cornerThickness, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.right - cornerThickness, frame.bottom
                - cornerLength, frame.right, frame.bottom, paint);

        // 绘制中间的线,每次刷新界面，中间的线往下移动scanningDistance
        slideTop += scanningDistance;
        if (slideTop >= frame.bottom) {
            slideTop = frame.top;
        }
        if (0x0 == laserStyle) {
            // Draw a colorful "laser scanner" line through the middle to show decoding is active
            paint.setColor(laserColor);
            paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
            canvas.drawRect(frame.left, slideTop, frame.right, slideTop + laserHeight, paint);
        } else if (0x1 == laserStyle) {
            Rect lineRect = new Rect();
            lineRect.left = frame.left;
            lineRect.right = frame.right;
            lineRect.top = slideTop;
            lineRect.bottom = slideTop + 18;
            canvas.drawBitmap(laserBitmap, null, lineRect, paint);
        }

        // 画扫描框下面的字
        paint.setColor(tipTextColor);
        paint.setTextSize(tipTextSize);
        paint.setAlpha(tipTextAlpha);
        paint.setTypeface(Typeface.DEFAULT);
        // paint.setTypeface(Typeface.create("System", Typeface.BOLD));
        float tip_text_width = paint.measureText(tipText);
        float tip_text_x = frame.left + (frame.width() - tip_text_width) / 2;
        float tip_text_y = (float) (frame.bottom + tipTextMarginTop);
        canvas.drawText(tipText, tip_text_x, tip_text_y, paint);

        // draw possible result points
        float scaleX = frame.width() / (float) previewFrame.width();
        float scaleY = frame.height() / (float) previewFrame.height();
        int frameLeft = frame.left;
        int frameTop = frame.top;
        lastPossibleResultPoints.clear();
        if (!possibleResultPoints.isEmpty()) {
            lastPossibleResultPoints.addAll(possibleResultPoints);
            paint.setAlpha(currentPointOpacity);
            paint.setColor(resultPointColor);
            synchronized (possibleResultPoints) {
                for (ResultPoint point : possibleResultPoints) {
                    canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                            frameTop + (int) (point.getY() * scaleY),
                            possiblePointSize, paint);
                }
            }
        }
        if (!lastPossibleResultPoints.isEmpty()) {
            paint.setAlpha(currentPointOpacity / 2);
            paint.setColor(resultPointColor);
            synchronized (lastPossibleResultPoints) {
                float radius = possiblePointSize / 2.0f;
                for (ResultPoint point : lastPossibleResultPoints) {
                    canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                            frameTop + (int) (point.getY() * scaleY),
                            radius, paint);
                }
            }
        }

        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(animationDelay,
                frame.left - possiblePointSize,
                frame.top - possiblePointSize,
                frame.right + possiblePointSize,
                frame.bottom + possiblePointSize);
    }

    public void drawViewfinder() {
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        synchronized (possibleResultPoints) {
            possibleResultPoints.add(point);
            int size = possibleResultPoints.size();
            if (size > maxResultPoints) {
                // trim it
                possibleResultPoints.subList(0, size - maxResultPoints / 2).clear();
            }
        }
    }

}
