package com.abborg.glom.utils;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ProgressBar;

public class ViewUtils {

    public static int convertDpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int convertPxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static void animateScale(View view, float from, float to, long duration) {
        Animation animation = new ScaleAnimation(from, to, from, to, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(duration);
        view.startAnimation(animation);
    }

    public static void animateRotate(View view, float from, float to, long duration) {
        Animation animation = new RotateAnimation(from, to, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setFillAfter(true);
        view.startAnimation(animation);
    }

    public static ObjectAnimator animateProgress(ProgressBar progressBar, int startValue, int endValue, long duration) {
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", startValue, endValue);
        animation.setDuration(duration);
        animation.setInterpolator(new LinearInterpolator());
        animation.start();
        return animation;
    }

    public static ObjectAnimator animateTranslateY(View view, int from, int to, long duration) {
        ObjectAnimator translation = ObjectAnimator.ofFloat(view, "translationY", from, to);
        translation.setDuration(duration);
        translation.start();
        return translation;
    }
}
