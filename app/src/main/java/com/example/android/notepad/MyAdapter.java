package com.example.android.notepad;

import android.content.Context;
import android.database.Cursor;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import androidx.cardview.widget.CardView;

public class MyAdapter extends SimpleCursorAdapter {
    private SparseBooleanArray selectedItems;

    public MyAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
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

        return view;
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
