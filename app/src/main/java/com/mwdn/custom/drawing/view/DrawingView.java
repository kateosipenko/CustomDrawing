package com.mwdn.custom.drawing.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.mwdn.custom.drawing.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private Layer currentLayer;
    private List<Layer> layers = new ArrayList<>();

    public DrawingView(Context context) {
        this(context, null);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public List<Layer> getLayers() {
        return layers;
    }

    public Layer addNewLayer() {
        currentLayer = Layer.newInstance();
        this.layers.add(currentLayer);
        invalidate();

        return currentLayer;
    }

    public void removeLayer(Layer layer) {
        if (layer != null) {
            int index = -1;
            for (int i = 0; i < layers.size(); i++) {
                Layer item = layers.get(i);
                if (item.getLayerId() == layer.getLayerId()) {
                    index = i;
                    break;
                }
            }

            if (index >= 0) {
                this.layers.remove(index);
                invalidate();
            }
        }
    }

    public void setCurrentLayer(Layer layer) {
        this.currentLayer = null;
        for (Layer item : layers) {
            if (item.getLayerId() == layer.getLayerId()) {
                currentLayer = item;
                break;
            }
        }

        invalidate();
    }

    public void removeLayerMode() {
        currentLayer.removeMode();
        invalidate();
    }

    public void setLayerMode(PorterDuff.Mode mode) {
        currentLayer.setLayerMode(mode);
        invalidate();
    }

    public void setMatrixType(@MatrixType int type) {
        currentLayer.setMatrixType(type);
        invalidate();
    }

    public void increaseStroke() {
        currentLayer.updateStroke(2f);
        invalidate();
    }

    public void decreaseStroke() {
        currentLayer.updateStroke(-2f);
        invalidate();
    }

    public void setPaintStyle(Paint.Style style) {
        currentLayer.paint.setStyle(style);
        invalidate();
    }

    public void setFillingType(@FillingType int type) {
        currentLayer.setFillingType(type, getResources(), getMeasuredWidth(), getMeasuredHeight());
        invalidate();
    }

    public void setMatrixTransform(@TransformType int type) {
        currentLayer.setTransformType(type);
        invalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                currentLayer.onEndMove(event.getX(), event.getY());
                invalidate();
                break;
        }

        gestureDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.GRAY);
        for (Layer layer : layers) {
            layer.onDraw(canvas);
        }
    }

    private void init() {
        addNewLayer();

        setWillNotDraw(false);
        initGestureDetector();
        initScaleDetector();
    }

    private void initGestureDetector() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                currentLayer.onStartMove(e.getX(), e.getY(), !currentLayer.path.isEmpty());
                invalidate();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                currentLayer.onMove(e2.getX(), e2.getY(), -distanceX, -distanceY, getMeasuredWidth(), getMeasuredHeight());
                invalidate();
                return super.onScroll(e1, e2, distanceX, distanceY);
            }
        });
    }

    private void initScaleDetector() {
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                currentLayer.scale(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                return super.onScale(detector);
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return super.onScaleBegin(detector);
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                super.onScaleEnd(detector);
            }
        });
    }

    public static class Layer {

        private static int NEXT_LAYER_ID = 0;

        private int layerId;

        private Path path = new Path();
        private Path drawingPath = new Path();

        private Paint paint;
        private Bitmap bitmap;
        private BitmapShader shader;

        private Rect imageRect;

        private Matrix layerMatrix = new Matrix();
        private Matrix pathMatrix = new Matrix();
        private Matrix paintMatrix = new Matrix();
        private Matrix shaderMatrix = new Matrix();

        @MatrixType
        private int matrixType = MatrixType.LAYER;
        @TransformType
        int transformType = TransformType.TRANSLATE;
        private boolean isMoving = false;

        private RectF pathBounds = new RectF();

        private PorterDuff.Mode currentMode;

        Layer() {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setStrokeWidth(4f);
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);

            layerId = NEXT_LAYER_ID;
            ++NEXT_LAYER_ID;
        }

        public Paint.Style getPaintStyle() {
            return paint.getStyle();
        }

        @MatrixType
        public int getMatrixType() {
            return matrixType;
        }

        @TransformType
        public int getTransformType() {
            return transformType;
        }

        static Layer newInstance() {
            return new Layer();
        }

        public int getLayerId() {
            return layerId;
        }

        public PorterDuff.Mode getLayerMode() {
            Xfermode xfermode = paint.getXfermode();
            return xfermode != null ? currentMode : null;
        }

        void setLayerMode(PorterDuff.Mode mode) {
            currentMode = mode;
            paint.setXfermode(new PorterDuffXfermode(mode));
        }

        void removeMode() {
            paint.setXfermode(null);
        }

        void updateStroke(float change) {
            paint.setStrokeWidth(paint.getStrokeWidth() + change);
        }

        void onDraw(Canvas canvas) {
            canvas.save();
            canvas.concat(layerMatrix);
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, null, imageRect, paint);
            }
            if (!drawingPath.isEmpty()) {
                canvas.drawPath(drawingPath, paint);
            } else {
                canvas.drawPath(path, paint);
            }

            canvas.restore();
        }

        private void onStartMove(float x, float y, boolean isMoving) {
            this.isMoving = isMoving;
            if (!isMoving) {
                path.reset();
                path.moveTo(x, y);
                path.lineTo(x, y);
            }
        }

        void setFillingType(@FillingType int type, Resources resources, int viewWidth, int viewHeight) {
            bitmap = null;
            switch (type) {
                case FillingType.COLOR:
                    paint.setShader(null);
                    paint.setColor(Color.RED);
                    break;
                case FillingType.IMAGE:
                    bitmap = BitmapFactory.decodeResource(resources, R.drawable.image);
                    imageRect = new Rect(0, 0, viewWidth, viewHeight);
                    paint.setShader(null);
                    break;
                case FillingType.SHADER:
                    Bitmap pattern = BitmapFactory.decodeResource(resources, R.drawable.pattern);
                    shader = new BitmapShader(pattern, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                    shader.setLocalMatrix(shaderMatrix);
                    paint.setShader(shader);
                    break;
            }
        }

        void setMatrixType(@MatrixType int matrixType) {
            this.matrixType = matrixType;
        }

        void setTransformType(int type) {
            this.transformType = type;
        }

        private void onMove(float x, float y, float dx, float dy, float viewWidth, float viewHeight) {
            if (isMoving) {
                Matrix matrix = getCurrentMatrix();
                switch (transformType) {
                    case TransformType.ROTATE:
                        path.computeBounds(pathBounds, true);
                        matrix.postRotate(dx, pathBounds.centerX(), pathBounds.centerY());
                        break;
                    case TransformType.SKEW:
                        matrix.postSkew(-dx / viewWidth, dy / viewHeight);
                        break;
                    case TransformType.TRANSLATE:
                        matrix.postTranslate(dx, dy);
                        break;
                }
                updateDrawnPath();
            } else {
                path.lineTo(x, y);
            }
        }

        private void scale(float scaleFactor, float focusX, float focusY) {
            getCurrentMatrix().setScale(scaleFactor, scaleFactor, focusX, focusY);
            updateDrawnPath();
        }

        private void onEndMove(float x, float y) {
            if (!isMoving) {
                path.lineTo(x, y);
                path.close();

                updateDrawnPath();
            }
        }

        private void updateDrawnPath() {
            path.transform(pathMatrix, drawingPath);
            if (shader != null) {
                shader.setLocalMatrix(shaderMatrix);
            }
        }

        private Matrix getCurrentMatrix() {
            switch (matrixType) {
                case MatrixType.PAINT:
                    return paintMatrix;
                case MatrixType.PATH:
                    return pathMatrix;
                case MatrixType.SHADER:
                    return shaderMatrix;
                case MatrixType.LAYER:
                default:
                    return layerMatrix;
            }
        }
    }

    @IntDef({FillingType.COLOR, FillingType.SHADER, FillingType.IMAGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FillingType {
        int COLOR = 0;
        int SHADER = 1;
        int IMAGE = 2;
    }

    @IntDef({MatrixType.LAYER, MatrixType.PATH, MatrixType.PAINT, MatrixType.SHADER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MatrixType {
        int LAYER = 0;
        int PATH = 1;
        int PAINT = 2;
        int SHADER = 3;
    }

    @IntDef({TransformType.TRANSLATE, TransformType.SKEW, TransformType.ROTATE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TransformType {
        int TRANSLATE = 0;
        int SKEW = 1;
        int ROTATE = 2;
    }
}