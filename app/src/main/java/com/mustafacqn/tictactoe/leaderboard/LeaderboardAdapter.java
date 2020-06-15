package com.mustafacqn.tictactoe.leaderboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.huawei.hms.jos.games.RankingsClient;
import com.huawei.hms.jos.games.ranking.RankingScore;
import com.mustafacqn.tictactoe.R;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<RankingScore> listItems;
    private Context context;

    public LeaderboardAdapter(List<RankingScore> listItems, Context context) {
        this.listItems = listItems;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.leaderboard_list_item, parent, false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RankingScore listItem = listItems.get(position);
        holder.textViewUserRank.setText(listItem.getDisplayRank());
        holder.textViewUserName.setText(listItem.getScoreOwnerDisplayName());
        holder.textViewUserPoint.setText(listItem.getRankingDisplayScore());

    }

    @Override
    public int getItemCount() { return listItems.size(); }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textViewUserRank;
        public TextView textViewUserName;
        public TextView textViewUserPoint;

        public ViewHolder(View itemView) {
            super(itemView);

            textViewUserRank = (TextView) itemView.findViewById(R.id.textView_user_rank);
            textViewUserName = (TextView) itemView.findViewById(R.id.textView_user_name);
            textViewUserPoint = (TextView) itemView.findViewById(R.id.textView_user_point);
        }
    }
}
