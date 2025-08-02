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
import android.graphics.LinearGradient;
import android.graphics.Shader;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import com.chibde.BaseVisualizer;

/**
 * CustomBarVisualizer creates a colorful bar visualizer with a fire-like gradient.
 * for the android {@link android.media.MediaPlayer}
 *
 * Created by gautam chibde on 29/10/17.
 */
public class CustomBarVisualizer extends BaseVisualizer {
    private Shader shader;

    public CustomBarVisualizer(Context context) {
        super(context);
    }

    public CustomBarVisualizer(Context context,
                                  @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomBarVisualizer(Context context,
                                  @Nullable AttributeSet attrs,
                                  int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        // The shader is now created in onSizeChanged where the view height is available
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h > 0) {
            // Create a fire-like gradient
            int[] fireGradient = {
                    Color.RED,
                    Color.YELLOW,
                    Color.RED
            };
            shader = new LinearGradient(0, 0, 0, h, fireGradient, null, Shader.TileMode.MIRROR);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bytes != null) {
            paint.setShader(shader);
            for (int i = 0, k = 0; i < (bytes.length - 1) && k < bytes.length; i++, k++) {
                int top = getHeight() +
                        ((byte) (Math.abs(bytes[k]) + 128)) * getHeight() / 128;
                canvas.drawLine(i, getHeight(), i, top, paint);
            }
            super.onDraw(canvas);
        }
    }
}

