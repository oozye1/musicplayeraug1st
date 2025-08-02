package com.chibde.visualizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import com.chibde.BaseVisualizer;

/**
 * GradientBlocksVisualizer creates a blocks visualizer with a vertical gradient
 * from green (bottom) to yellow, orange, and red (top).
 */
public class GradientBlocksVisualizer extends BaseVisualizer {
    private int numBlocks = 120; // Increased number of blocks for smaller size
    private Paint paint;

    public GradientBlocksVisualizer(Context context) {
        super(context);
        init();
    }

    public GradientBlocksVisualizer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bytes == null) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        int blockWidth = width / numBlocks;
        for (int i = 0; i < numBlocks; i++) {
            int byteIndex = i * bytes.length / numBlocks;
            int blockHeight = (int) ((bytes[byteIndex] + 128) * height / 256f);
            int left = i * blockWidth;
            int top = height - blockHeight;
            int right = left + blockWidth - 2;
            int bottom = height;
            // Create gradient for each block
            Shader shader = new LinearGradient(
                left, bottom, left, top,
                new int[]{0xFF00FF00, 0xFFFFFF00, 0xFFFFA500, 0xFFFF0000},
                new float[]{0f, 0.33f, 0.66f, 1f},
                Shader.TileMode.CLAMP
            );
            paint.setShader(shader);
            canvas.drawRect(left, top, right, bottom, paint);
        }
        paint.setShader(null);
    }
}
