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
 * SpikeVisualizer creates a visualization with sharp, triangular spikes.
 *
 * Created by gautam chibde on 29/10/17.
 */
public class SpikeVisualizer extends BaseVisualizer {
    private int numSpikes = 50;

    public SpikeVisualizer(Context context) {
        super(context);
    }

    public SpikeVisualizer(Context context,
                           @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SpikeVisualizer(Context context,
                           @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bytes != null) {
            float spikeWidth = getWidth() / (float) numSpikes;
            for (int i = 0; i < numSpikes; i++) {
                int byteIndex = (int) Math.ceil(i * (bytes.length / (float) numSpikes));
                float magnitude = (float) Math.hypot(bytes[byteIndex], 0);
                float height = magnitude / 128f * getHeight();

                Path spikePath = new Path();
                spikePath.moveTo(i * spikeWidth, getHeight());
                spikePath.lineTo((i + 0.5f) * spikeWidth, getHeight() - height);
                spikePath.lineTo((i + 1) * spikeWidth, getHeight());
                spikePath.close();
                canvas.drawPath(spikePath, paint);
            }
        }
        super.onDraw(canvas);
    }
}

