package com.altf4.figuremortis;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<DisplayItem> displayItems;
    private final OnClickListener onClickListener;

    public HistoryAdapter(List<DisplayItem> displayItems, OnClickListener onClickListener) {
        this.displayItems = displayItems;
        this.onClickListener = onClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == DisplayItem.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_year_header, parent, false);
            return new YearHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
            return new PersonViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = displayItems.get(position);
        if (holder.getItemViewType() == DisplayItem.TYPE_HEADER) {
            ((YearHeaderViewHolder) holder).yearTextView.setText(item.getYear());
        } else {
            PersonViewHolder personHolder = (PersonViewHolder) holder;
            personHolder.personName.setText(item.getDeath().getText());
            personHolder.itemView.setOnClickListener(v -> onClickListener.onItemClick(item.getDeath()));
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    public static class PersonViewHolder extends RecyclerView.ViewHolder {
        TextView personName;

        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            personName = itemView.findViewById(R.id.personName);
        }
    }

    public static class YearHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView yearTextView;

        public YearHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            yearTextView = itemView.findViewById(R.id.yearTextView);
        }
    }

    public interface OnClickListener {
        void onItemClick(Death death);
    }
}