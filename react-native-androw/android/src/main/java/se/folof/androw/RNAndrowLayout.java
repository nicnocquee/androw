package se.folof.androw;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewParent;

import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.views.view.ReactViewGroup;

import android.support.annotation.NonNull;

public class RNAndrowLayout extends ReactViewGroup {

    public RNAndrowImageListener imageListener;

    private int mColor;
    private float mRadius;
    private float mOpacity;
    private float dX;
    private float dY;
    private float x;
    private float y;

    private Bitmap shadow = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    private Bitmap content = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blur = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Canvas draw = new Canvas(content);

    private boolean contentDirty;
    private boolean shadowDirty;
    private boolean hasContent;
    private boolean hasOpacity;
    private boolean hasRadius;
    private boolean hasColor;
    private boolean hasArea;

    public RNAndrowLayout(Context context) {
        super(context);
    }

    public void setShadowOffset(ReadableMap map) {
        boolean hasMap = map != null;

        if (hasMap && map.hasKey("width")) {
            dX = (float) map.getDouble("width");
        } else {
            dX = 0f;
        }

        if (hasMap && map.hasKey("height")) {
            dY = (float) map.getDouble("height");
        } else {
            dY = 0f;
        }

        dX = dX*this.getContext().getResources().getDisplayMetrics().density;
        dY = dY*this.getContext().getResources().getDisplayMetrics().density;

        super.invalidate();
    }

    public void setShadowColor(Integer color) {
        hasColor = color != null;
        if (hasColor && mColor != color) {
            paint.setColor(color);
            paint.setAlpha(Math.round(255 * mOpacity));
            mColor = color;
        }
        super.invalidate();
    }

    public void setShadowOpacity(Dynamic Opacity) {
        hasOpacity = Opacity != null && !Opacity.isNull();
        float opacity = hasOpacity ? (float) Opacity.asDouble() : 0f;
        hasOpacity &= opacity > 0f;
        if (hasOpacity && mOpacity != opacity) {
            paint.setColor(mColor);
            paint.setAlpha(Math.round(255 * opacity));
            mOpacity = opacity;
        }
        super.invalidate();
    }

    public void setShadowRadius(Dynamic Radius) {
        hasRadius = Radius != null && !Radius.isNull();
        float rawRadius = hasRadius ? (float) Radius.asDouble() : 0f;
        float radius = (rawRadius*2)*this.getContext().getResources().getDisplayMetrics().density;
        hasRadius &= radius > 0f;

        if (hasRadius && mRadius != radius ) {
            blur.setMaskFilter(new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL));
            mRadius = radius;
            shadowDirty = true;

        }
        super.invalidate();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ViewParent invalidateChildInParent(final int[] location, final Rect dirty) {
        contentDirty = true;
        shadowDirty = true;
        return super.invalidateChildInParent(location, dirty);
    }

    @Override
    public void onDescendantInvalidated(@NonNull View child, @NonNull View target) {
        contentDirty = true;
        shadowDirty = true;
        super.onDescendantInvalidated(child, target);
        super.invalidate(); //Gives better effect on touchableopacity etc.
    }

    @Override
    public void invalidate() {
        contentDirty = true;
        shadowDirty = true;
        super.invalidate();
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onMeasure(int widthSpec, int heightSpec) {
        int height = MeasureSpec.getSize(heightSpec);
        int width = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(width, height);
        hasArea = width > 0 && height > 0;

        if (hasArea) {
            if (content.getWidth() == width && content.getHeight() == height) {
                return;
            }
            content.recycle();
            hasContent = false;
            content = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            draw.setBitmap(content);
        }
        invalidate();
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        if (hasArea) {
            if (contentDirty) {
                if (hasContent) {
                    content.eraseColor(Color.TRANSPARENT);
                }
                super.dispatchDraw(draw);
                contentDirty = false;
                hasContent = true;
            }

            if (hasColor && hasOpacity) {
                if (shadowDirty) {
                    shadow.recycle();
                    shadow = content.extractAlpha(blur, null);

                    shadowDirty = false;
                }

                x = dX-((shadow.getWidth()-content.getWidth())/2);
                y = dY-((shadow.getHeight()-content.getHeight())/2);

                canvas.drawBitmap(shadow, x, y, paint);
            }
            canvas.drawBitmap(content, 0f, 0f, null);
        } else {
            super.dispatchDraw(canvas);
        }

    }
}