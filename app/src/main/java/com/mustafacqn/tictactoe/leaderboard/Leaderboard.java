package com.mustafacqn.tictactoe.leaderboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.games.GameScopes;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.RankingsClient;
import com.huawei.hms.jos.games.ranking.Ranking;
import com.huawei.hms.jos.games.ranking.RankingScore;
import com.huawei.hms.support.api.entity.auth.Scope;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.result.HuaweiIdAuthResult;
import com.mustafacqn.tictactoe.R;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class Leaderboard extends Activity {

    private static final String TAG = "Leaderboard";
    private static final String GAME_PLAYED_NUMBER_MULTIPLAYER  = "7A716F6B89A365E0D188DEA920D0C3D4D352829CE56121738C9CCA3AFB0286CA";
    private static final String SINGLE_PLAYER_POINTS            = "67D684BFFA54DC93F7DE0A92B493AA6FFB8B30139E73393B206603FC3168DF46";
    private AuthHuaweiId hwAccount;
    private HuaweiIdAuthParams authParams;
    private RankingsClient rankingsClient;
    private String rankingId;
    private List<Scope> scopes;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private List<RankingScore> rankingScore;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leaderboard_list);

        hwAccount = getIntent().getParcelableExtra("hw_account");
        driveSignin();

        JosAppsClient appsClient = JosApps.getJosAppsClient(this, hwAccount);
        appsClient.init();

        scopes = new ArrayList<>();
        scopes.add(GameScopes.DRIVE_APP_DATA);
        authParams = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME)
                .setScopeList(scopes).createParams();


//        initViews();


    }

    private void checkLeaderboards(){
        rankingsClient = Games.getRankingsClient(this, hwAccount);
        boolean isRealTime = true;
        rankingsClient.getRankingSummary(SINGLE_PLAYER_POINTS, isRealTime).addOnSuccessListener(new OnSuccessListener<Ranking>() {
            @Override
            public void onSuccess(Ranking ranking) {
                Log.i(TAG, "onSuccess: Ranking retrieval Success");
                rankingId = ranking.getRankingId();
                showLeaderboard();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "onFailure, Code: " + ((ApiException)e).getStatusCode());
            }
        });
    }

    private void showLeaderboard(){

        int timeDimension = 0;
        int pageDirection = 0;
        int maxResults = 21;
        int offsetPlayerRank = 0;

//        Task<RankingsClient.RankingScores> task = rankingsClient.getRankingTopScores(rankingId, timeDimension, maxResults, offsetPlayerRank, pageDirection);
        Task<RankingsClient.RankingScores> task = rankingsClient.getMoreRankingScores(rankingId, offsetPlayerRank, maxResults, pageDirection, timeDimension);
        task.addOnSuccessListener(new OnSuccessListener<RankingsClient.RankingScores>() {
            @Override
            public void onSuccess(RankingsClient.RankingScores rankingScores) {
                rankingScore = rankingScores.getRankingScores();
                initViews(); // define adapter
                recyclerView.setAdapter(adapter);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "onFailure Code: " + ((ApiException)e).getStatusCode() );
                Toast.makeText(getApplicationContext(), "Please open Huawei Game Services", Toast.LENGTH_LONG);
                finish();
            }
        });
    }

    private void driveSignin() {
        Log.i(TAG, "driveSignin: init success");

        Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.getService(this, authParams).silentSignIn();
        authHuaweiIdTask.addOnSuccessListener(new OnSuccessListener<AuthHuaweiId>() {
            @Override
            public void onSuccess(AuthHuaweiId authHuaweiId) {
                Log.i(TAG, "onSuccess: signIn success" + authHuaweiId.getDisplayName());
                hwAccount = authHuaweiId;
                checkLeaderboards();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException){
                    Log.e(TAG, "onFailure: signIn failed Code: " + ((ApiException)e).getStatusCode());
                    loudSignin();
                }
            }
        });
    }

    private static final int LOUD_SIGN_IN_INTENT = 3000;
    private void loudSignin(){
        Intent intent = HuaweiIdAuthManager.getService(this, authParams).getSignInIntent();
        startActivityForResult(intent, LOUD_SIGN_IN_INTENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOUD_SIGN_IN_INTENT) {
            if (null == data) {
                Log.e(TAG, "onActivityResult: intent is null");
                return;
            }
            String jsonSigninResult = data.getStringExtra("HUAWEIID_SIGNIN_RESULT");
            if (TextUtils.isEmpty(jsonSigninResult)) {
                Log.e(TAG, "onActivityResult: SigninResult is empty");
                return;
            }
            try {
                HuaweiIdAuthResult signinResult = new HuaweiIdAuthResult().fromJson(jsonSigninResult);
                if (0 == signinResult.getStatus().getStatusCode()) {
                    Log.i(TAG, "onActivityResult: Signin Success!" + signinResult.toJson());
                    hwAccount = signinResult.getHuaweiId();
                    checkLeaderboards();
                }else {
                    Log.e(TAG, "onActivityResult: Signin Failed!" + signinResult.getStatus().getStatusCode() );
                }
            }catch (JSONException var){
                Log.e(TAG, "onActivityResult: Failed to convert json");
            } catch (Exception e){
                Log.e(TAG, "onActivityResult: Other Exceptions");
            }

        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.leaderboard_recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(rankingScore, this);
        recyclerView.setAdapter(adapter);
    }

}
