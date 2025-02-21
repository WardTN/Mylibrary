package com.dq.mylibrary.recy;


import android.content.Context;
import android.util.SparseBooleanArray;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 支持多选的RecyclerView.Adapter
 */
public abstract class MultiSelectAdapter<T, VH extends RecyclerView.ViewHolder> extends BaseRecyAdapter<T, VH> {
    private SparseBooleanArray selectedItems;

    public MultiSelectAdapter(Context context, List<T> dataList) {
        super(context, dataList);
        selectedItems = new SparseBooleanArray();
    }

    public void toggleSelection(int position) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public boolean isSelected(int position) {
        return selectedItems.get(position, false);
    }
}