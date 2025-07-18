package com.altf4.figuremortis;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeathsAdapter extends RecyclerView.Adapter<DeathsAdapter.ViewHolder> {

    private final List<Death> deaths;
    private final OnClickListener onClickListener;

    public DeathsAdapter(List<Death> deaths, OnClickListener onClickListener) {
        this.deaths = deaths;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Death death = deaths.get(position);
        holder.personName.setText(death.getText());
        holder.personYear.setText(death.getYear());

        holder.itemView.setOnClickListener(v -> onClickListener.onItemClick(death));
    }

    @Override
    public int getItemCount() {
        return deaths.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView personName;
        TextView personYear;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            personName = itemView.findViewById(R.id.personName);
            personYear = itemView.findViewById(R.id.personYear);
        }
    }

    public interface OnClickListener {
        void onItemClick(Death death);
    }
}
