package com.piebrowser.ui.splash;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.piebrowser.R;
import com.piebrowser.browser.BrowserActivity;
import com.piebrowser.ui.theme.ThemeManager;

/**
 * SplashActivity — animated launch screen.
 *
 * Shows the Pie Browser logo + name with a smooth
 * scale + fade animation before entering the browser.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 1800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splashLogo);
        TextView appName = findViewById(R.id.splashAppName);
        TextView tagline = findViewById(R.id.splashTagline);

        // Start animations
        animateLogo(logo);
        animateText(appName, 300);
        animateText(tagline, 600);

        // Navigate to browser after delay
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, BrowserActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION_MS);
    }

    private void animateLogo(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.5f);
        view.setScaleY(0.5f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, scaleX, scaleY);
        set.setDuration(700);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    private void animateText(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(30f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(view, "translationY", 30f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, slideUp);
        set.setDuration(500);
        set.setStartDelay(delay);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }
}
