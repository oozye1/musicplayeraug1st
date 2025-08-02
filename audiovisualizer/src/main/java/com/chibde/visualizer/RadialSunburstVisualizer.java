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
 * RadialSunburstVisualizer creates a sunburst effect that radiates from the center.
 *
 * Created by gautam chibde on 29/10/17.
 */
public class RadialSunburstVisualizer extends BaseVisualizer {
    private float[] points;
    private float[] fft;

    public RadialSunburstVisualizer(Context context) {
        super(context);
    }

    public RadialSunburstVisualizer(Context context,
                                    @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RadialSunburstVisualizer(Context context,
                                    @Nullable AttributeSet attrs,
                                    int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bytes != null) {
            if (points == null || points.length < bytes.length * 4) {
                points = new float[bytes.length * 4];
            }

            float angle = 0;
            for (int i = 0; i < bytes.length - 1; i++, angle += (360f / (bytes.length - 1))) {
                float x = (float) (getWidth() / 2 + Math.cos(Math.toRadians(angle)) * (100 + bytes[i]));
                float y = (float) (getHeight() / 2 + Math.sin(Math.toRadians(angle)) * (100 + bytes[i]));

                points[i * 4] = getWidth() / 2f;
                points[i * 4 + 1] = getHeight() / 2f;
                points[i * 4 + 2] = x;
                points[i * 4 + 3] = y;
            }
            canvas.drawLines(points, paint);
        }
        super.onDraw(canvas);
    }
}

