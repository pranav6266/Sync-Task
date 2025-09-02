package com.pranav.synctask.utils;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public static String formatDate(Timestamp timestamp) {
        if (timestamp == null) return "";
        return dateFormat.format(timestamp.toDate());
    }

    public static String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "";
        return timeFormat.format(timestamp.toDate());
    }

    public static boolean isToday(Timestamp timestamp) {
        if (timestamp == null) return false;

        Calendar today = Calendar.getInstance();
        Calendar taskDate = Calendar.getInstance();
        taskDate.setTime(timestamp.toDate());

        return today.get(Calendar.YEAR) == taskDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == taskDate.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isThisMonth(Timestamp timestamp) {
        if (timestamp == null) return false;

        Calendar today = Calendar.getInstance();
        Calendar taskDate = Calendar.getInstance();
        taskDate.setTime(timestamp.toDate());

        return today.get(Calendar.YEAR) == taskDate.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == taskDate.get(Calendar.MONTH);
    }
}