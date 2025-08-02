/*
* Copyright (C) 2017 Gautam Chibde
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.chibde.visualizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import com.chibde.BaseVisualizer;

/**
 * FadingBlocksVisualizer creates a visualization with blocks that fade in and out.
 *
 * Created by gautam chibde on 29/10/17.
 */
public class FadingBlocksVisualizer extends BaseVisualizer {
    private int numBlocks = 16;

    public FadingBlocksVisualizer(Context context) {
        super(context);
    }

    public FadingBlocksVisualizer(Context context,
                                  @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FadingBlocksVisualizer(Context context,
                                  @Nullable AttributeSet attrs,
                                  int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bytes != null) {
            int blockWidth = getWidth() / numBlocks;
            int blockHeight = getHeight() / numBlocks;

            for (int i = 0; i < numBlocks; i++) {
                for (int j = 0; j < numBlocks; j++) {
                    int byteIndex = (i * numBlocks + j) % bytes.length;
                    int alpha = (int) (Math.abs(bytes[byteIndex]) * 2);
                    paint.setColor(Color.argb(alpha, i * 255 / numBlocks, j * 255 / numBlocks, 255));
                    canvas.drawRect(i * blockWidth, j * blockHeight, (i + 1) * blockWidth, (j + 1) * blockHeight, paint);
                }
            }
        }
        super.onDraw(canvas);
    }
}

