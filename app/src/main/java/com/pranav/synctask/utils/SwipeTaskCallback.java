package com.pranav.synctask.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.pranav.synctask.R;

public abstract class SwipeTaskCallback extends ItemTouchHelper.SimpleCallback {

    private final Context context;
    private final Drawable iconDelete;
    private final Drawable iconComplete;
    private final ColorDrawable background;
    private final int backgroundColorDelete;
    private final int backgroundColorComplete;
    private final Paint clearPaint;

    public SwipeTaskCallback(Context context) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;

        // Load resources
        iconDelete = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        iconDelete.setTint(0xFFFFFFFF); // White

        iconComplete = ContextCompat.getDrawable(context, R.drawable.ic_check_white);

        background = new ColorDrawable();
        backgroundColorDelete = ContextCompat.getColor(context, R.color.md_theme_light_error); // Red
        backgroundColorComplete = ContextCompat.getColor(context, R.color.priority_low); // Green

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false; // We don't support drag & drop sorting
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        boolean isCancelled = dX == 0 && !isCurrentlyActive;

        if (isCancelled) {
            clearCanvas(c, itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        // Draw Background & Icon
        if (dX > 0) {
            // Swiping to the Right (Complete)
            background.setColor(backgroundColorComplete);
            background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
            background.draw(c);

            int iconMargin = (itemHeight - iconComplete.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + iconComplete.getIntrinsicHeight();
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = iconLeft + iconComplete.getIntrinsicWidth();

            iconComplete.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            iconComplete.draw(c);

        } else if (dX < 0) {
            // Swiping to the Left (Delete)
            background.setColor(backgroundColorDelete);
            background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            background.draw(c);

            int iconMargin = (itemHeight - iconDelete.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + iconDelete.getIntrinsicHeight();
            int iconRight = itemView.getRight() - iconMargin;
            int iconLeft = iconRight - iconDelete.getIntrinsicWidth();

            iconDelete.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            iconDelete.draw(c);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        c.drawRect(left, top, right, bottom, clearPaint);
    }
}