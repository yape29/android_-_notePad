package com.example.android.notepad;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

import androidx.cardview.widget.CardView;

import java.util.Arrays;

public class MyAdapter extends SimpleCursorAdapter {
    private SparseBooleanArray selectedItems;
    private Context context; // 保存context
    private boolean isClassifyView; // 添加标志来区分是否是分类视图
    public MyAdapter(Context context, int layout, Cursor c, String[] from, int[] to, boolean isClassifyView) {
        super(context, layout, c, from, to);
        this.context = context;
        this.isClassifyView = isClassifyView;
        selectedItems = new SparseBooleanArray();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        // 获取每个 CardView
        CardView cardView = (CardView) view.findViewById(R.id.cardView);

        // 根据选中状态更改背景颜色
        if (selectedItems.get(position, false)) {
            cardView.setCardBackgroundColor(view.getContext().getResources().getColor(R.color.selected_color));
        } else {
            cardView.setCardBackgroundColor(view.getContext().getResources().getColor(R.color.default_color));
        }
        // 只在笔记视图中处理收藏按钮
        if (!isClassifyView) {
            ImageView favoriteButton = (ImageView) view.findViewById(R.id.favoriteButton);
            if (favoriteButton != null) {
                handleFavoriteButtonClick(favoriteButton, position);
            }
        }
        return view;
    }

    private void handleFavoriteButtonClick(final ImageView favoriteButton, final int position) {
        final Cursor cursor = (Cursor) getItem(position);
        final long noteId = cursor.getLong(cursor.getColumnIndex(NotePad.Notes._ID));

        // 确保cursor包含star列
        int starColumnIndex = cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_STAR);
        if (starColumnIndex != -1) {
            final int isFavorite = cursor.getInt(starColumnIndex);

            // 设置收藏图标
            if (isFavorite == 1) {
                favoriteButton.setImageResource(R.drawable.android_star);
            } else {
                favoriteButton.setImageResource(R.drawable.android_notstar);
            }

            // 设置点击事件
            favoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int newStarStatus = (isFavorite == 1) ? 0 : 1;

                    if (newStarStatus == 1) {
                        favoriteButton.setImageResource(R.drawable.android_star);
                    } else {
                        favoriteButton.setImageResource(R.drawable.android_notstar);
                    }

                    ContentValues values = new ContentValues();
                    values.put(NotePad.Notes.COLUMN_NAME_STAR, newStarStatus);
                    Uri updateUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, noteId);
                    context.getContentResolver().update(updateUri, values, null, null);

                    notifyDataSetChanged();
                }
            });
        }
    }

    // 设置选中项
    public void setItemSelected(int position, boolean selected) {
        if (selected) {
            selectedItems.put(position, true);
        } else {
            selectedItems.delete(position);
        }
        notifyDataSetChanged(); // 刷新列表视图
    }

    // 清空选中项
    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }




}
