package com.dq.mylibrary.recy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public abstract class BaseRecyAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected List<T> dataList;
    protected LayoutInflater inflater;
    protected OnItemClickListener listener;

    public BaseRecyAdapter(Context context, List<T> dataList) {
        this.dataList = dataList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public T getItem(int position) {
        return dataList.get(position);
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }


    public void bindClick(View holder, int position) {
        if (listener != null) {
            holder.setOnClickListener(v -> listener.onItemClick(v, position));
        }
    }
}