package com.mustafacqn.tictactoe.achievements;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.huawei.hms.jos.games.achievement.Achievement;
import com.mustafacqn.tictactoe.R;

import java.util.List;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.ViewHolder> {

    private List<Achievement> listItems;
    private Context context;

    public AchievementsAdapter(List<Achievement> listItems, Context context) {
        this.listItems = listItems;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.achievement_list_item, parent, false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Achievement listItem = listItems.get(position);
//        holder.textViewHead.setText(listItem.getHead());
//        holder.textViewDesc.setText(listItem.getDesc());
//        Uri thumbnailUri = listItem.getReachedThumbnailUri();
        Glide.with(context).load(listItem.getReachedThumbnailUri()).into(holder.imageViewThumbnail);
        holder.textViewHead.setText(listItem.getDisplayName());
        holder.textViewDesc.setText(listItem.getDescInfo());
//        holder.imageViewThumbnail.setText(listItem.getId());
//        holder.imageViewThumbnail.setImageURI(thumbnailUri);
    }


    @Override
    public int getItemCount() {
        return listItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textViewHead;
        public TextView textViewDesc;
        public ImageView imageViewThumbnail;

        public ViewHolder(View itemView) {
            super(itemView);

            textViewHead = (TextView) itemView.findViewById(R.id.textView_itemHead);
            textViewDesc = (TextView) itemView.findViewById(R.id.textView_itemDesc);
            imageViewThumbnail = (ImageView) itemView.findViewById(R.id.imageView_itemThumbnail);
        }
    }
}
