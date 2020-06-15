package com.mustafacqn.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;
import com.mustafacqn.tictactoe.achievements.Achievements;
import com.mustafacqn.tictactoe.leaderboard.Leaderboard;
import com.mustafacqn.tictactoe.multiplayer.NearbyMainPage;
import com.mustafacqn.tictactoe.save.SaveGame;

public class Opening extends AppCompatActivity{

    private final String TAG = "OpeningClass";

    public static final int REQUEST_SIGN_IN_LOGIN = 1002;
    public static final int REQUEST_ACHIEVEMENTS_LOGIN = 1555;
    public static final int REQUEST_MULTIPLAYER_LOGIN = 1453;
    public static final int REQUEST_LOADGAME_LOGIN = 1071;
    public static final int REQUEST_LEADERBOARD_LOGIN = 2023;

    HuaweiIdAuthService mAuthManager;
    HuaweiIdAuthParams mAuthParam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.opening);

        mAuthParam = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
                .setIdToken()
                .createParams();
        mAuthManager = HuaweiIdAuthManager.getService(Opening.this, mAuthParam);
    }


    public void nearbyMulti(View view) { // nearbyMulti ClickHandler
        startActivityForResult(mAuthManager.getSignInIntent(), REQUEST_MULTIPLAYER_LOGIN);
    }

    public void nearbyMultiStart(Intent intent) { startActivity(intent); }

    public void loadGameStart(Intent intent) { startActivity(intent); }

    public void loadGame(View view) {
        startActivityForResult(mAuthManager.getSignInIntent(), REQUEST_LOADGAME_LOGIN);
    }

    public void leaderboard(View view){
        leaderboardStart(new Intent(this, Leaderboard.class));
    }

    public void leaderboardStart(Intent intent){
        startActivity(intent);
    }

    public void achievements(View view) {
        startActivityForResult(mAuthManager.getSignInIntent(), REQUEST_ACHIEVEMENTS_LOGIN);
    }

    public void achievementsStart (Intent intent) {
        startActivity(intent);
    }

    public void gameStart(Intent intent) {
        startActivity(intent);
    }

    public void huaweiIdStart(View view) {
        startActivityForResult(mAuthManager.getSignInIntent(), REQUEST_SIGN_IN_LOGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data);

            if(authHuaweiIdTask.isSuccessful()) {
                AuthHuaweiId huaweiAccount = authHuaweiIdTask.getResult();
                Intent intent;
                switch (requestCode){
                    case REQUEST_SIGN_IN_LOGIN:
                        intent = new Intent(this, MainActivity.class);
                        intent.putExtra("hw_account", huaweiAccount);
                        gameStart(intent);
                        break;
                    case REQUEST_ACHIEVEMENTS_LOGIN:
                        intent = new Intent(this, Achievements.class);
                        intent.putExtra("hw_account", huaweiAccount);
                        achievementsStart(intent);
                        break;
                    case REQUEST_MULTIPLAYER_LOGIN:
                        intent = new Intent(this, NearbyMainPage.class);
                        intent.putExtra("hw_account", huaweiAccount);
                        nearbyMultiStart(intent);
                        break;
                    case REQUEST_LOADGAME_LOGIN:
                        intent = new Intent(this, SaveGame.class);
                        intent.putExtra("hw_account", huaweiAccount);
                        loadGameStart(intent);
                        break;
                    case REQUEST_LEADERBOARD_LOGIN:
                        intent = new Intent(this, Leaderboard.class);
                        intent.putExtra("hw_account", huaweiAccount);
                        leaderboardStart(intent);
                        break;
                    default:
                        Log.e(TAG, "onActivityResult: No case founded! check switch-case!");
                        break;
                }
            }else {
                Log.i(TAG, "signIn Failed: " + authHuaweiIdTask.getException());
            }
        }
}
