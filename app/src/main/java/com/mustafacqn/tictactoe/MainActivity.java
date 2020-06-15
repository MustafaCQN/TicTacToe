package com.mustafacqn.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.games.AchievementsClient;
import com.huawei.hms.jos.games.ArchivesClient;
import com.huawei.hms.jos.games.EventsClient;
import com.huawei.hms.jos.games.GameScopes;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.RankingsClient;
import com.huawei.hms.jos.games.archive.ArchiveDetails;
import com.huawei.hms.jos.games.archive.ArchiveSummary;
import com.huawei.hms.jos.games.archive.ArchiveSummaryUpdate;
import com.huawei.hms.jos.games.event.Event;
import com.huawei.hms.jos.games.ranking.ScoreSubmissionInfo;
import com.huawei.hms.support.api.entity.auth.Scope;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.result.HuaweiIdAuthResult;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;

import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "MainActivity";
    private final String achievementRankUp = "688C709EA75C4F927E4FC2B5DE6AA6978E33056818D014CA7004A8A7A31E7EB5";
    private static final String SINGLE_PLAYER_POINTS = "67D684BFFA54DC93F7DE0A92B493AA6FFB8B30139E73393B206603FC3168DF46";

    private Button[][] buttons = new Button[3][3];

    private boolean player1Turn = true;

    private int roundCount = 0;

    private int player1Points;
    private int player2Points;
    private AchievementsClient achievementsClient;
    private AuthHuaweiId hwAccount;

    private TextView textViewPlayer1;
    private TextView textViewPlayer2;

    private HuaweiIdAuthParams huaweiIdAuthParams;
    private List<Scope> scopes;
    private long playedTime;
    public RankingsClient rankingsClient;

    public EventsClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewPlayer1 = findViewById(R.id.text_view_p1);
        textViewPlayer2 = findViewById(R.id.text_view_p2);

        scopes = new ArrayList<>();
        scopes.add(GameScopes.DRIVE_APP_DATA);
        huaweiIdAuthParams = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME)
                .setScopeList(scopes).createParams();

        // setting buttons onclick listeners
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String buttonID = "button_" + i + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);
                buttons[i][j].setOnClickListener(this);
            }
        }

        // start appclient to use game service features
        JosAppsClient appsClient = JosApps.getJosAppsClient(this, hwAccount);
        appsClient.init();

        Log.i(TAG, "driveSignin: init success");

        checkForLoadGame();
        assignLeaderboards();

        Button buttonReset = findViewById(R.id.button_reset);
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetGame();
            }
        });

        Button saveGame = findViewById(R.id.button_saveGame);
        saveGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveGame();
            }
        });

        // get name for salute
        hwAccount = getIntent().getParcelableExtra("hw_account");
        String toastWithUserNameString = hwAccount == null ? null : hwAccount.getDisplayName();
        Log.i(TAG, "Username: " + toastWithUserNameString);
        if(toastWithUserNameString != null){
            Toast.makeText(getApplicationContext(), "Welcome " + toastWithUserNameString + "!", Toast.LENGTH_SHORT).show();
            playedTime = new Date().getTime();
        }else {
            Toast.makeText(getApplicationContext(), "Never saw you, HEY WHO ARE YOU!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.button_reset) {
            Log.i(TAG, "onClick: ");
        }

        if (!(((Button)v).getText().toString().equals(""))) {
            return;
        }

        if (player1Turn) {
//            v.setBackgroundColor(getResources().getColor(R.color.playerOne));
            ((Button) v).setText("X");
        } else {
//            v.setBackgroundColor(getResources().getColor(R.color.playerTwo));
            ((Button) v).setText("O");
        }

        roundCount++;

        if (checkForWin()) {
            submitScoreToLeaderboards();
            if (player1Turn) {
                player1Wins();
            } else {
                player2Wins();
            }
        } else if (roundCount == 9) {
            draw();
        } else {
            player1Turn = !player1Turn;
        }

    }

    private void checkForLoadGame() {
        if(getIntent().getStringExtra("buttons") != null){
            // 0-> buttons array (XOYOXOY)
            // 1-> Player1Points(int), 2-> Player2Points(int)
            // 3-> player1Turn(boolean), 4-> roundCount(int)
            String buttons = getIntent().getStringExtra("buttons");
            int player1Points = Integer.parseInt(getIntent().getStringExtra("player_1_points"));
            int player2Points = Integer.parseInt(getIntent().getStringExtra("player_2_points"));
            boolean player1Turns = Boolean.parseBoolean(getIntent().getStringExtra("player_1_turn"));
            int roundCount = Integer.parseInt(getIntent().getStringExtra("round_count"));

            assignButtonsFromStringArray(buttons);
            this.player1Points = player1Points;
            this.player2Points = player2Points;
            this.player1Turn = player1Turns;
            this.roundCount = roundCount;

            updatePointsText();

            if(player1Turns){
                Toast.makeText(this, "X's turn", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this, "O's turn", Toast.LENGTH_LONG).show();
            }

        }else {
            Log.i(TAG, "checkForLoadGame: This is not a load game");
        }
    }

    private void assignButtonsFromStringArray(String buttons){
        String [] buttonsArray = buttons.split("");
        int k = 0;
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                switch (buttonsArray[k]){
                    case "X":
                        this.buttons[i][j].setText("X");
                        k++;
                        continue;
                    case "Y":
                        this.buttons[i][j].setText("O");
                        k++;
                        continue;
                    case "O":
                        this.buttons[i][j].setText("");
                        k++;
                        continue;
                    default:
                        Log.e(TAG, "assignButtonsFromStringArray: There is an error in switch");
                        k++;
                        j--;
                }
            }
        }
    }

    private boolean checkForWin() {
        String [][] field = new String [3][3];

        for(int i = 0; i < 3; i++) {
            for (int j = 0; j < 3 ; j++){
                field[i][j] = buttons[i][j].getText().toString();
            }
        }

        for (int i = 0; i < 3; i++) {
            if (field[i][0].equals(field[i][1]) && field[i][0].equals(field[i][2])
                    && !field[i][0].equals("")) {
                return true;
            }
        }

        for (int i = 0; i < 3; i++) {
            if (field[0][i].equals(field[1][i]) && field[0][i].equals(field[2][i])
                    && !field[0][i].equals("")) {
                return true;
            }
        }

        if (field[0][0].equals(field[1][1])
                && field[0][0].equals(field[2][2])
                && !field[0][0].equals("")) {
            return true;
        }

        if (field[0][2].equals(field[1][1])
                && field[0][2].equals(field[2][0])
                && !field[0][2].equals("")) {
            return true;
        }

        return false;
    }

    private void unlockAchievement() {
        // TODO: AchievementsClient objesi kullanılarak hidden olan bir achievementi reveal etmek gerekiyor
        // achievementRankUp Stringi (localde tanımlı) ve visualize veya visualizeWithResult kullanarak
        // ekranda hidden bir achievementi reveal et. https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/game-achievement

        // String userName = getIntent().getStringExtra("user_name");
        hwAccount = getIntent().getParcelableExtra("hw_account");
        if( hwAccount != null){
            JosAppsClient appsClient = JosApps.getJosAppsClient(this, hwAccount);
            appsClient.init();

            achievementsClient = Games.getAchievementsClient(this, hwAccount);
            Task<Void> hiddenAchievement = achievementsClient.visualizeWithResult(achievementRankUp);
            hiddenAchievement.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // TODO: burada reveal edilen achievementsi ekrana bas böylece kullanıcı hangi achievementi açtığını görsün.
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    if(e instanceof ApiException) {
                        Log.i(TAG, "onFailureUnlockAchievement: " + ((ApiException)e).getStatusCode());
                    }
                }
            });
        }
    }

    private void player1Wins() {
        player1Points++;
        if(player1Points == 3) {
            unlockAchievement();
        }
        Toast.makeText(this, "X wins!", Toast.LENGTH_SHORT).show();
        updatePointsText();
        resetBoard();
    }

    private void player2Wins() {
        player2Points++;
        if (player2Points == 5){
            triggerEvent();
        }
        Toast.makeText(this, "O wins!", Toast.LENGTH_SHORT).show();
        updatePointsText();
        resetBoard();
    }

    private void triggerEvent() {
        if (hwAccount.isExpired()){
            silentSignin();
            triggerEvent();
            return;
        }
        client = Games.getEventsClient(this, hwAccount);
        client.getEventList(true).addOnSuccessListener(events -> {
            if (events == null){
                Log.e(TAG, "onSuccess: Events is null");
            }
            for (Event e : events){
                Log.i(TAG, "onSuccess: Event, EventId: " + e.getEventId());
                if (e.getName().equals("Bloody Screen")){
                    client.grow(e.getEventId(), 1);
                }
            }
        }).addOnFailureListener(e -> {
            if (e instanceof ApiException){
                Log.e(TAG, "onFailure: events Error" + ((ApiException)e).getStatusCode());
            }else {
                Log.e(TAG, "onFailure: events Error" + e.getMessage());
            }
        });

    }

    private void draw() {
        Toast.makeText(this, "Draw!", Toast.LENGTH_SHORT).show();
        // TODO: oyun bittikten sonra çıkan desenin gözükmesi için reset atmadan önce uygulamayı 1-2 saniyeliğine durdur.
        resetBoard();
    }

    private void updatePointsText() {
        textViewPlayer1.setText("X " + player1Points);
        textViewPlayer2.setText(player2Points + " O");
    }

    private void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
            }
        }

        roundCount = 0;
        player1Turn = true;
    }

    private void resetGame() {
        player1Points = 0;
        player2Points = 0;
        updatePointsText();
        resetBoard();
    }

    public String buttonsToString() {

        StringBuilder stringBuilder = new StringBuilder();
//        int p1Color = getResources().getColor(R.color.playerOne);
//        int p2Color = getResources().getColor(R.color.playerTwo);

        for(int i = 0; i<3; i++){
            for(int j = 0; j < 3; j++){
                if (buttons[i][j].getText().toString().equals("X")){
                    stringBuilder.append("X");
                }else if (buttons[i][j].getText().toString().equals("O")){
                    stringBuilder.append("Y");
                }else {
                    stringBuilder.append("O");
                }
            }
        }
        return stringBuilder.toString();
    }

    private void driveSignin() {


        Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.getService(this, huaweiIdAuthParams).silentSignIn();
        authHuaweiIdTask.addOnSuccessListener(new OnSuccessListener<AuthHuaweiId>() {
            @Override
            public void onSuccess(AuthHuaweiId authHuaweiId) {
                Log.i(TAG, "onSuccess: signIn success" + authHuaweiId.getDisplayName());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException){
                    Log.e(TAG, "onFailure: signIn failed" + ((ApiException)e).getStatusCode());
                    loudSignin();
                }
            }
        });
    }

    public byte[] getSavedData () {
        StringBuilder builder = new StringBuilder();
        builder.append(buttonsToString()+",")
                .append(player1Points+",")
                .append(player2Points+",")
                .append(player1Turn+",")
                .append(roundCount);
        return builder.toString().getBytes();
    }

    private void saveGame() {

        driveSignin();

        if (hwAccount != null && hwAccount.isExpired()){
            silentSignin();
            saveGame();
        }else if(hwAccount != null){
            ArchivesClient client = Games.getArchiveClient(this, hwAccount);
            ArchiveDetails detail = new ArchiveDetails.Builder().build();
            // TODO: How we can fill this set method with byte array (i guess its the information about the game)
            detail.set(getSavedData());
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-YY - hh:mm");
            boolean isSupportCache = false;

            ArchiveSummaryUpdate archiveSummaryUpdate =
                    new ArchiveSummaryUpdate.Builder()
                            .setActiveTime(new Date().getTime() - playedTime)
                            .setDescInfo("Game - " + dateFormat.format(date))
                            .build();
            Task<ArchiveSummary> addArchiveTask = client.addArchive(detail, archiveSummaryUpdate, isSupportCache);
            addArchiveTask.addOnSuccessListener((archiveSummary -> {
                if (archiveSummary != null){
                    String fileName = archiveSummary.getFileName();
                    String achieveId = archiveSummary.getId();
                    Log.i(TAG, "saveGame: SUCCESS fileName: " + fileName + " achieveId: " + achieveId);
                    Toast.makeText(this, "Saved Successfully", Toast.LENGTH_LONG).show();
                }
            })).addOnFailureListener((e -> {
                if (e instanceof ApiException) {
                    Log.e(TAG, "saveGame: ERROR statusCode" + ((ApiException)e).getStatusCode());
                }
            }));
        }
    }

    private void silentSignin() {
        HuaweiIdAuthService mAuthManager = HuaweiIdAuthManager.getService(this, huaweiIdAuthParams);
        Task<AuthHuaweiId> authHuaweiIdTask = mAuthManager.silentSignIn();
        authHuaweiIdTask
                .addOnSuccessListener(authHuaweiId -> hwAccount = authHuaweiId)
                .addOnFailureListener(e ->{
                    hwAccount = null;
                    Log.e(TAG, "silentSignin: Code: " + ((ApiException)e).getStatusCode() );
                });
    }

    private static final int LOUD_SIGN_IN_INTENT = 3000;
    private void loudSignin(){
        Intent intent = HuaweiIdAuthManager.getService(this, huaweiIdAuthParams).getSignInIntent();
        startActivityForResult(intent, LOUD_SIGN_IN_INTENT);
    }

    public void submitScoreToLeaderboards() {
        rankingsClient.submitScoreWithResult(SINGLE_PLAYER_POINTS, player1Turn ? player1Points : player2Points).addOnSuccessListener(new OnSuccessListener<ScoreSubmissionInfo>() {
            @Override
            public void onSuccess(ScoreSubmissionInfo scoreSubmissionInfo) {
                Log.i(TAG, "onSuccess: Submitting the score is successful, playerid: " + scoreSubmissionInfo.getPlayerId() + "  & rankingid: " + scoreSubmissionInfo.getRankingId());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException){
                    Log.e(TAG, "onFailure: code: " + ((ApiException) e).getStatusCode());
                }else {
                    Log.e(TAG, "onFailure: message: " + e.getMessage());
                }
            }
        });
    }

    public void assignLeaderboards() {
        rankingsClient = Games.getRankingsClient(this, getIntent().getParcelableExtra("hw_account"));
        rankingsClient.getRankingSwitchStatus().addOnSuccessListener(integer -> {
            Log.e(TAG, "LEADERBOARD IS WORKING FINE");
            if (integer == 0){
                rankingsClient.setRankingSwitchStatus(1).addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.i(TAG, "onSuccess: setSwitch is success!");
                    }
                }).addOnFailureListener(e -> {
                    if (e instanceof ApiException){
                        Log.e(TAG, "onFailure: There is an error in setSwitch, Code:" + ((ApiException)e).getStatusCode() );
                    }else{
                        Log.e(TAG, "onFailure: There is an error in setSwitch, Message:" + e.getMessage());
                    }
                });
            }else {
                Log.i(TAG, "LEADERBOARD SWITCH STATUS IS NOT 0 ALREADY !! status: " + integer);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException){
                    Log.e(TAG, "onFailure Code: " + ((ApiException)e).getStatusCode() );
                }else{
                    Log.e(TAG, "onFailure Message: " + e.getMessage());
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String [] buttonColorArray = new String [9];
        int k = 0;
        for(int i = 0; i<3; i++){
            for(int j = 0; j < 3; j++){
                buttonColorArray[k++] = buttons[i][j].getText().toString();
            }
        }


        outState.putStringArray("buttonColorArray", buttonColorArray);
        outState.putInt("roundCount", roundCount);
        outState.putInt("player1Points", player1Points);
        outState.putInt("player2Points", player2Points);
        outState.putBoolean("player1Turn", player1Turn);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        roundCount = savedInstanceState.getInt("roundCount");
        player1Points = savedInstanceState.getInt("player1Points");
        player2Points = savedInstanceState.getInt("player2Points");
        player1Turn = savedInstanceState.getBoolean("player1Turn");
        String [] buttonColorArray = savedInstanceState.getStringArray("buttonColorArray");
        int k=0;
        for(int i = 0; i<3 ;i++){
            for(int j = 0; j < 3; j++){
                buttons[i][j].setText(buttonColorArray[k++]);
            }
        }
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
                }else {
                    Log.e(TAG, "onActivityResult: Signin Failed!" + signinResult.getStatus().getStatusCode() );
                }
            }catch (JSONException var){

            }

        }
    }
}