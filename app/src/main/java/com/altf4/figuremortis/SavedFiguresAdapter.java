package com.altf4.figuremortis;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.altf4.figuremortis.db.DatabaseHelper;
import com.altf4.figuremortis.service.GeminiService;

import java.util.List;

public class SavedFiguresAdapter extends RecyclerView.Adapter<SavedFiguresAdapter.ViewHolder> {

    private final List<GeminiService.GroundedResponse> figures;
    private final OnClickListener onClickListener;

    public SavedFiguresAdapter(List<GeminiService.GroundedResponse> figures, OnClickListener onClickListener) {
        this.figures = figures;
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
        GeminiService.GroundedResponse figure = figures.get(position);
        holder.personName.setText(figure.name);

        holder.itemView.setOnClickListener(v -> onClickListener.onItemClick(figure));
    }

    @Override
    public int getItemCount() {
        return figures.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView personName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            personName = itemView.findViewById(R.id.personName);
        }
    }

    public interface OnClickListener {
        void onItemClick(GeminiService.GroundedResponse figure);
    }
}
