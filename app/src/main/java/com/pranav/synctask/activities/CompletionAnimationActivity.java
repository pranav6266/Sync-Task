package com.pranav.synctask.activities;

import android.animation.Animator;
import android.media.MediaPlayer; // 1. ADD THIS IMPORT
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.pranav.synctask.R;

public class CompletionAnimationActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer; // 2. ADD THIS FIELD

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completion);

        LottieAnimationView lottieAnimationView = findViewById(R.id.lottiecompleteanimation);

        // 3. CREATE AND CONFIGURE THE MEDIA PLAYER
        // (Replace 'task_complete_tune' with your actual audio file name)
        mediaPlayer = MediaPlayer.create(this, R.raw.task_complete_tune);
        mediaPlayer.setLooping(false); // Ensure it only plays once

        lottieAnimationView.setVisibility(View.VISIBLE);

        // 4. START BOTH AT THE SAME TIME
        lottieAnimationView.playAnimation();
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }

        lottieAnimationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {}

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                // 5. CLEAN UP and finish
                cleanUpMediaPlayer();
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {
                // 5. CLEAN UP and finish
                cleanUpMediaPlayer();
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {}
        });
    }

    // 6. ADD THIS HELPER METHOD FOR CLEANUP
    private void cleanUpMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release(); // Release the media player resources
            mediaPlayer = null;
        }
    }

    // 7. ADD ONDESTROY FOR SAFETY
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure we clean up if the activity is destroyed unexpectedly
        cleanUpMediaPlayer();
    }

    @Override
    public void onBackPressed() {
        // Do nothing. Prevent user from closing the animation early. [cite: 150]
        super.onBackPressed();
    }
}