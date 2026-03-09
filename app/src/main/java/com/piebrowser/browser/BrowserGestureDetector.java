package com.piebrowser.browser;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * BrowserGestureDetector
 *
 * Swipe gestures:
 * - Swipe RIGHT → go back
 * - Swipe LEFT  → go forward
 * - Swipe DOWN  → reload page
 *
 * Configurable from Settings → Gestures & Shortcuts.
 */
public class BrowserGestureDetector extends GestureDetector.SimpleOnGestureListener {

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private static final int EDGE_ZONE = 80;  // px from edge to trigger edge swipe

    public interface SwipeListener { void onSwipe(); }

    private final GestureDetector detector;
    private SwipeListener onSwipeLeft;
    private SwipeListener onSwipeRight;
    private SwipeListener onSwipeDown;

    public BrowserGestureDetector(Context context) {
        detector = new GestureDetector(context, this);
        detector.setIsLongpressEnabled(true);
    }

    public void onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2,
                           float velocityX, float velocityY) {
        if (e1 == null || e2 == null) return false;

        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();

        // Only trigger if horizontal swipe is dominant
        if (Math.abs(diffX) > Math.abs(diffY)) {
            if (Math.abs(diffX) > SWIPE_THRESHOLD
                    && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    // Swipe right = go back
                    if (onSwipeRight != null) onSwipeRight.onSwipe();
                } else {
                    // Swipe left = go forward
                    if (onSwipeLeft != null) onSwipeLeft.onSwipe();
                }
                return true;
            }
        } else {
            // Vertical swipe
            if (diffY > SWIPE_THRESHOLD
                    && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                // Swipe down = reload
                if (onSwipeDown != null) onSwipeDown.onSwipe();
                return true;
            }
        }
        return false;
    }

    public void setOnSwipeLeftListener(SwipeListener l) { onSwipeLeft = l; }
    public void setOnSwipeRightListener(SwipeListener l) { onSwipeRight = l; }
    public void setOnSwipeDownListener(SwipeListener l) { onSwipeDown = l; }
}
