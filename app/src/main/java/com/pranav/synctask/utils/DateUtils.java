package com.pranav.synctask.utils;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    // Returns "Today", "Tomorrow", "Yesterday" or "Oct 24"
    public static String getRelativeDate(Timestamp timestamp) {
        if (timestamp == null) return "";

        long now = System.currentTimeMillis();
        long time = timestamp.toDate().getTime();

        // Android's built-in helper handles "Today"/"Yesterday" logic automatically
        return android.text.format.DateUtils.getRelativeTimeSpanString(
                time,
                now,
                android.text.format.DateUtils.DAY_IN_MILLIS,
                android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString();
    }

    public static String formatDate(Timestamp timestamp) {
        if (timestamp == null) return "";
        return dateFormat.format(timestamp.toDate());
    }

    // Check if a specific timestamp is strictly before today (Overdue)
    public static boolean isOverdue(Timestamp timestamp) {
        if (timestamp == null) return false;

        Calendar taskDate = Calendar.getInstance();
        taskDate.setTime(timestamp.toDate());

        // Reset task time to midnight to compare dates only
        taskDate.set(Calendar.HOUR_OF_DAY, 0);
        taskDate.set(Calendar.MINUTE, 0);
        taskDate.set(Calendar.SECOND, 0);
        taskDate.set(Calendar.MILLISECOND, 0);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return taskDate.before(today);
    }
}