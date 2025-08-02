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
import android.graphics.Path;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import com.chibde.BaseVisualizer;

/**
 * WaveVisualizer creates a wave-like visualization of the audio.
 *
 * Created by gautam chibde on 29/10/17.
 */
public class WaveVisualizer extends BaseVisualizer {
    private Path wavePath;
    private float phase = 0f;
    private float amplitude = 20f;

    public WaveVisualizer(Context context) {
        super(context);
    }

    public WaveVisualizer(Context context,
                          @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WaveVisualizer(Context context,
                          @Nullable AttributeSet attrs,
                          int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        paint.setColor(Color.CYAN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        wavePath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bytes != null) {
            wavePath.reset();
            wavePath.moveTo(0, getHeight() / 2);

            float maxAmplitude = 0;
            for (byte aByte : bytes) {
                if (Math.abs(aByte) > maxAmplitude) {
                    maxAmplitude = Math.abs(aByte);
                }
            }
            amplitude = maxAmplitude / 128 * (getHeight() / 4);

            for (int i = 0; i < getWidth(); i++) {
                float y = (float) (getHeight() / 2 + amplitude * Math.sin(2 * Math.PI * (i / (float) getWidth()) + phase));
                wavePath.lineTo(i, y);
            }

            canvas.drawPath(wavePath, paint);
            phase += 0.1f;
            invalidate();
        }
        super.onDraw(canvas);
    }
}

