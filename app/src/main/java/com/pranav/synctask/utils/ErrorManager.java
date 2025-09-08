package com.pranav.synctask.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
public class ErrorManager {
    public static void handleFirebaseError(Exception e, Context context) {
        String message;
        if (e instanceof FirebaseNetworkException) {
            message = "Please check your internet connection";
        } else if (e instanceof FirebaseAuthException) {
            message = "Authentication error. Please sign in again";
        } else {
            message = "Something went wrong. Please try again";
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
