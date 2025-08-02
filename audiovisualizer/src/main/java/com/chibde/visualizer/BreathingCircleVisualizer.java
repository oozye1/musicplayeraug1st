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
 * BreathingCircleVisualizer creates a circle that expands and contracts with the music.
 *
 * Created by gautam chibde on 29/10/17.
 */
public class BreathingCircleVisualizer extends BaseVisualizer {
    private float radius;

    public BreathingCircleVisualizer(Context context) {
        super(context);
    }

    public BreathingCircleVisualizer(Context context,
                                     @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BreathingCircleVisualizer(Context context,
                                     @Nullable AttributeSet attrs,
                                     int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        paint.setColor(Color.MAGENTA);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bytes != null) {
            float maxMagnitude = 0;
            for (byte aByte : bytes) {
                if (Math.abs(aByte) > maxMagnitude) {
                    maxMagnitude = Math.abs(aByte);
                }
            }
            radius = maxMagnitude / 128f * (Math.min(getWidth(), getHeight()) / 2f);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, paint);
        }
        super.onDraw(canvas);
    }
}

