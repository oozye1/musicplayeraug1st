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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * FountainVisualizer creates a fountain-like effect of particles.
 *
 * Created by gautam chibde on 29/10/17.
 */
public class FountainVisualizer extends BaseVisualizer {
    private List<Particle> particles;
    private Random random = new Random();

    public FountainVisualizer(Context context) {
        super(context);
    }

    public FountainVisualizer(Context context,
                              @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FountainVisualizer(Context context,
                              @Nullable AttributeSet attrs,
                              int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        paint.setStyle(Paint.Style.FILL);
        particles = new ArrayList<>();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bytes != null) {
            if (particles.size() < 200) {
                for (int i = 0; i < 10; i++) {
                    int index = random.nextInt(bytes.length - 1);
                    float magnitude = (float) Math.hypot(bytes[index], bytes[index + 1]);
                    if (magnitude > 10) {
                        particles.add(new Particle(getWidth() / 2f, getHeight(), magnitude));
                    }
                }
            }

            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update();
                if (p.isDead()) {
                    particles.remove(i);
                } else {
                    paint.setColor(p.color);
                    canvas.drawCircle(p.x, p.y, p.radius, paint);
                }
            }
            invalidate();
        }
        super.onDraw(canvas);
    }

    private class Particle {
        float x, y;
        float vx, vy;
        int color;
        float radius;
        int age;

        Particle(float x, float y, float magnitude) {
            this.x = x;
            this.y = y;
            this.vx = (random.nextFloat() - 0.5f) * 10;
            this.vy = -magnitude / 8f - random.nextFloat() * 10;
            this.color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
            this.radius = random.nextFloat() * 4 + 2;
            this.age = 0;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.5f; // gravity
            age++;
        }

        boolean isDead() {
            return y > getHeight() || age > 100;
        }
    }
}

