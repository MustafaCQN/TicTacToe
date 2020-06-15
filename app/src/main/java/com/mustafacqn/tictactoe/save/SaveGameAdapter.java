package com.mustafacqn.tictactoe.save;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.huawei.hms.jos.games.archive.ArchiveSummary;
import com.mustafacqn.tictactoe.R;

import java.util.List;

public class SaveGameAdapter extends RecyclerView.Adapter<SaveGameAdapter.ViewHolder> {

    private List<ArchiveSummary> archiveSummaries;
    private Context context;
    private SaveGame saveGame;

    public SaveGameAdapter(List<ArchiveSummary> archiveSummaries, Context context, SaveGame saveGame) {
        this.archiveSummaries = archiveSummaries;
        this.context = context;
        this.saveGame = saveGame;
    }

    @NonNull
    @Override
    public SaveGameAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.save_game_list_item, parent, false);

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull SaveGameAdapter.ViewHolder holder, int position) {
        ArchiveSummary archiveSummary = archiveSummaries.get(position);
        holder.saveName.setText(archiveSummary.getFileName());
        holder.description.setText(archiveSummary.getDescInfo());
        holder.playTime.setText(Long.toString(archiveSummary.getActiveTime()));
        holder.layout.setOnClickListener(saveGame);
    }

    @Override
    public int getItemCount() {
        return archiveSummaries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView saveName;
        public TextView description;
        public TextView playTime;
        public LinearLayout layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            saveName = itemView.findViewById(R.id.save_nameTW);
            description = itemView.findViewById(R.id.save_descriptionTW);
            playTime = itemView.findViewById(R.id.play_time);
            layout = itemView.findViewById(R.id.save_linear_layout);
        }
    }
}
