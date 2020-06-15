package com.mustafacqn.tictactoe.achievements;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.games.AchievementsClient;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.achievement.Achievement;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.mustafacqn.tictactoe.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Achievements extends AppCompatActivity{

    private final String TAG = "Achievements Log";

    AchievementsClient achievementsClient;
    private final boolean forceReloadServer = true;
    private Achievements mContext;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private List<Achievement> listItems;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.achievements);

        Log.i(TAG, "Achievements START");
        mContext = this;
        listItems = new ArrayList<>();
        initViews();
        requestData();



    }


    public void requestData () {
        // get huaweiId from the parent activity to process with it
        AuthHuaweiId huaweiId = getIntent().getParcelableExtra("hw_account");
        Log.i(TAG, "huaweiId : " + huaweiId.toString());

        JosAppsClient appsClient = JosApps.getJosAppsClient(this, huaweiId);
        appsClient.init();

        // get achievementclient using huaweiId
        achievementsClient = Games.getAchievementsClient(this, huaweiId);

        // get the achievements list from client, true = obtain from server, false = obtain from client
        Task<List<Achievement>> achievementsList = achievementsClient.getAchievementList(false);
        achievementsList.addOnSuccessListener(new OnSuccessListener<List<Achievement>>() {
            @Override
            public void onSuccess(List<Achievement> achievements) {
                if(achievements == null) {
                    Log.i(TAG, "achievementBuffer is null");
                    return;
                }else {
                    Log.i(TAG, "achievementBuffer is not null");
                }
                // Do the screen parsing thing for achievements
                // Adapter kullan
                // TODO: listItems = achievements yeter. Düzelt şunu.
                Iterator iterator = achievements.iterator();
                listItems.clear();
                while (iterator.hasNext()) {
                    Achievement achievement = (Achievement) iterator.next();
                    listItems.add(achievement);
                }
                recyclerView.setAdapter(adapter);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if(e instanceof ApiException) {
                    Log.i(TAG, "rtnCode: " + ((ApiException)e).getStatusCode());
                    Toast.makeText(mContext, "Please open Huawei Game Services", Toast.LENGTH_LONG);
                    finish();
                }
            }
        });
    }



    public void initViews(){
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        adapter = new AchievementsAdapter(listItems, mContext);
        recyclerView.setAdapter(adapter);
    }
}
