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
 * DotMatrixVisualizer creates a visualization with a grid of dots that react to the music.
 *
 * Created by gautam chibde on 29/10/17.
 */
public class DotMatrixVisualizer extends BaseVisualizer {
    private int rows = 16;
    private int cols = 16;

    public DotMatrixVisualizer(Context context) {
        super(context);
    }

    public DotMatrixVisualizer(Context context,
                               @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DotMatrixVisualizer(Context context,
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
            float cellWidth = getWidth() / (float) cols;
            float cellHeight = getHeight() / (float) rows;
            float radius;

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int byteIndex = (i * cols + j) % bytes.length;
                    float magnitude = (float) Math.hypot(bytes[byteIndex], 0);
                    radius = magnitude / 128f * Math.min(cellWidth, cellHeight) / 2f;

                    paint.setColor(Color.rgb(i * 255 / rows, j * 255 / cols, 255));
                    canvas.drawCircle(j * cellWidth + cellWidth / 2, i * cellHeight + cellHeight / 2, radius, paint);
                }
            }
        }
        super.onDraw(canvas);
    }
}

