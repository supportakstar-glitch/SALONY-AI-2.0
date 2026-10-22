package com.akstarcompany.salony.ai.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class AiOrbView extends View {

    public enum State {
        IDLE,
        LISTENING,
        PROCESSING,
        SPEAKING,
        INTERRUPTED,
        ERROR
    }

    private State currentState = State.IDLE;
    private float rmsLevel = 0f; // 0.0 to 1.0

    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private long startTime;

    public AiOrbView(Context context) {
        super(context);
        init();
    }

    public AiOrbView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AiOrbView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        startTime = SystemClock.uptimeMillis();
        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(3f);
        particlePaint.setStyle(Paint.Style.FILL);
    }

    public void setState(State state) {
        this.currentState = state;
        postInvalidateOnAnimation();
    }

    public State getState() {
        return currentState;
    }

    public void setRmsLevel(float rmsdB) {
        // Normalize rmsdB (-2 to 10 typical) to 0.0 - 1.0
        this.rmsLevel = Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f));
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float cx = width / 2f;
        float cy = height / 2f;
        float baseRadius = Math.min(width, height) * 0.28f;

        long elapsed = SystemClock.uptimeMillis() - startTime;
        float timeSec = elapsed / 1000f;

        // Color configurations based on State
        int coreColor1;
        int coreColor2;
        int haloColor;

        switch (currentState) {
            case LISTENING:
                coreColor1 = Color.parseColor("#00E5FF"); // Bright Cyan
                coreColor2 = Color.parseColor("#E91E63"); // Pink
                haloColor = Color.argb(90, 0, 229, 255);
                break;
            case PROCESSING:
                coreColor1 = Color.parseColor("#FF4081"); // Pink
                coreColor2 = Color.parseColor("#7C4DFF"); // Purple
                haloColor = Color.argb(100, 255, 64, 129);
                break;
            case SPEAKING:
                coreColor1 = Color.parseColor("#FF4081"); // Hot Pink
                coreColor2 = Color.parseColor("#C2185B"); // Deep Pink
                haloColor = Color.argb(120, 255, 64, 129);
                break;
            case ERROR:
                coreColor1 = Color.parseColor("#FF5252"); // Red
                coreColor2 = Color.parseColor("#D50000");
                haloColor = Color.argb(100, 255, 82, 82);
                break;
            case INTERRUPTED:
                coreColor1 = Color.parseColor("#FFB300"); // Amber
                coreColor2 = Color.parseColor("#E91E63");
                haloColor = Color.argb(80, 255, 179, 0);
                break;
            case IDLE:
            default:
                coreColor1 = Color.parseColor("#FF4081"); // Sweet Rose
                coreColor2 = Color.parseColor("#380B26"); // Burgundy
                haloColor = Color.argb(70, 233, 30, 99);
                break;
        }

        // Dynamic radius calculation
        float pulse = 0f;
        if (currentState == State.IDLE) {
            pulse = (float) Math.sin(timeSec * 2.0) * (baseRadius * 0.06f);
        } else if (currentState == State.LISTENING) {
            pulse = rmsLevel * (baseRadius * 0.35f) + (float) Math.sin(timeSec * 5.0) * (baseRadius * 0.08f);
        } else if (currentState == State.PROCESSING) {
            pulse = (float) Math.sin(timeSec * 6.0) * (baseRadius * 0.12f);
        } else if (currentState == State.SPEAKING) {
            pulse = (float) Math.abs(Math.sin(timeSec * 8.0)) * (baseRadius * 0.22f);
        }

        float currentRadius = baseRadius + pulse;

        // 1. Draw Outer Glowing Halo
        float haloRadius = currentRadius * 1.55f;
        RadialGradient haloShader = new RadialGradient(
                cx, cy, haloRadius,
                new int[]{haloColor, Color.TRANSPARENT},
                new float[]{0.3f, 1.0f},
                Shader.TileMode.CLAMP
        );
        haloPaint.setShader(haloShader);
        canvas.drawCircle(cx, cy, haloRadius, haloPaint);

        // 2. Draw Ripples when Speaking or Listening
        if (currentState == State.SPEAKING || currentState == State.LISTENING) {
            for (int i = 1; i <= 3; i++) {
                float waveOffset = ((timeSec * 1.5f + (i * 0.33f)) % 1f);
                float rippleR = currentRadius + waveOffset * (baseRadius * 0.6f);
                int alpha = (int) ((1f - waveOffset) * 160);
                ripplePaint.setColor(coreColor1);
                ripplePaint.setAlpha(alpha);
                canvas.drawCircle(cx, cy, rippleR, ripplePaint);
            }
        }

        // 3. Draw Orbiting Energy Particles in PROCESSING state
        if (currentState == State.PROCESSING) {
            particlePaint.setColor(Color.WHITE);
            for (int i = 0; i < 6; i++) {
                double angle = (timeSec * 3.5) + (i * (Math.PI / 3.0));
                float px = cx + (float) Math.cos(angle) * (currentRadius * 1.25f);
                float py = cy + (float) Math.sin(angle) * (currentRadius * 1.25f);
                canvas.drawCircle(px, py, 6f, particlePaint);
            }
        }

        // 4. Draw Core Glowing Orb
        RadialGradient coreShader = new RadialGradient(
                cx - currentRadius * 0.2f, cy - currentRadius * 0.25f, currentRadius,
                new int[]{Color.WHITE, coreColor1, coreColor2},
                new float[]{0.05f, 0.45f, 1.0f},
                Shader.TileMode.CLAMP
        );
        corePaint.setShader(coreShader);
        canvas.drawCircle(cx, cy, currentRadius, corePaint);

        // Continuous lightweight animation request
        postInvalidateOnAnimation();
    }
}
