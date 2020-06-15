package com.mustafacqn.tictactoe.multiplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
//import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.RankingsClient;
import com.huawei.hms.jos.games.ranking.Ranking;
import com.huawei.hms.nearby.Nearby;
import com.huawei.hms.nearby.StatusCode;
import com.huawei.hms.nearby.discovery.BroadcastOption;
import com.huawei.hms.nearby.discovery.ConnectCallback;
import com.huawei.hms.nearby.discovery.ConnectInfo;
import com.huawei.hms.nearby.discovery.ConnectResult;
import com.huawei.hms.nearby.discovery.DiscoveryEngine;
import com.huawei.hms.nearby.discovery.Policy;
import com.huawei.hms.nearby.discovery.ScanEndpointCallback;
import com.huawei.hms.nearby.discovery.ScanEndpointInfo;
import com.huawei.hms.nearby.discovery.ScanOption;
import com.huawei.hms.nearby.transfer.Data;
import com.huawei.hms.nearby.transfer.DataCallback;
import com.huawei.hms.nearby.transfer.TransferEngine;
import com.huawei.hms.nearby.transfer.TransferStateUpdate;
import com.mustafacqn.tictactoe.R;
import com.mustafacqn.tictactoe.leaderboard.Leaderboard;

import java.nio.charset.Charset;
import java.util.List;

public class NearbyMulti extends Activity implements View.OnClickListener {

    private static final String TAG = "NearbyMulti";
    private static final String LEADERBOARD_ID = "7A716F6B89A365E0D188DEA920D0C3D4D352829CE56121738C9CCA3AFB0286CA";
    private TextView textViewPlayer1;
    private TextView textViewPlayer2;
    Context context;
    private String serviceId;
    public boolean isScanner = false;
    private Button[][] buttons = new Button[3][3];
    private Button resetButton;
    //    private int initialButtonColor;
    private boolean player1Turn = true;
    private boolean isPlayer1;
    private int roundCount = 0;
    public TransferEngine transferEngine;
    private int player1Points = 0;
    private int player2Points = 0;
    public RelativeLayout waitLayout;
    public String mEndPointId;
    public String myName;
    public String friendsName;
    int resetNumber = 0;
    public boolean myReset = false;
    public boolean friendReset = false;
    public RankingsClient rankingsClient;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multiplayer_game);

        context = getApplicationContext();
//        transferEngine = Nearby.getTransferEngine(context);

        textViewPlayer1 = findViewById(R.id.text_view_p1);
        textViewPlayer2 = findViewById(R.id.text_view_p2);
        waitLayout = findViewById(R.id.waitLayout);
        resetButton = findViewById(R.id.button_reset_m);
        resetButton.setOnClickListener(this);

        Intent intent = getIntent();
        myName = intent.getStringExtra("myname");
        friendsName = intent.getStringExtra("friendsname");

        assignLeaderboards();

        NearbyConnection nearbyConnect = new NearbyConnection();
        if(myName.compareTo(friendsName) > 0){
            serviceId = myName + friendsName;
            isScanner = true;
            isPlayer1 = true;
            textViewPlayer1.setText(getString(R.string.textViewPlayerRevised, myName, player1Points));
            textViewPlayer2.setText(getString(R.string.textViewPlayerRevised, friendsName, player2Points));
            nearbyConnect.startScanning();
        }else if (myName.compareTo(friendsName) < 0) {
            serviceId = friendsName + myName;
            isScanner = false;
            isPlayer1 = false;
            textViewPlayer1.setText(getString(R.string.textViewPlayerRevised, friendsName, player2Points));
            textViewPlayer2.setText(getString(R.string.textViewPlayerRevised, myName, player1Points));
            nearbyConnect.startBroadcasting();
        } else {
            Log.e(TAG, "startConnection: Names have to be different");
            Toast.makeText(context, "Names have to be different", Toast.LENGTH_LONG).show();
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String buttonID = "button_" + i + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);
                buttons[i][j].setOnClickListener(this);
            }
        }

//        initialButtonColor = ((ColorDrawable)(buttons[0][0].getBackground())).getColor();

    }

    public void checkforReset() {
        if ((myReset && !friendReset) || (!myReset && friendReset)) {
            findViewById(R.id.button_reset_m).setBackgroundColor(getResources().getColor(R.color.resetOnePlayer));
        } else if (myReset && friendReset){ // Done
            findViewById(R.id.button_reset_m).setBackgroundColor(getResources().getColor(R.color.resetTwoPlayer));
            resetBoard();
            player1Points = 0;
            player2Points = 0;
            updatePointsText();
            findViewById(R.id.button_reset_m).setBackgroundColor(getResources().getColor(R.color.resetDefault));
            myReset = false;
            friendReset = false;
            player1Turn = true;
        }else {
            Log.e(TAG, "checkforReset: Unknown reset status, please debug");
            findViewById(R.id.button_reset_m).setBackgroundColor(getResources().getColor(R.color.resetDefault));
            myReset = false;
            friendReset = false;
        }
    }

    public void assignLeaderboards() {
        rankingsClient = Games.getRankingsClient(this, getIntent().getParcelableExtra("hw_account"));
        rankingsClient.getRankingSwitchStatus().addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                if (integer == 0){
                    rankingsClient.setRankingSwitchStatus(1).addOnSuccessListener(new OnSuccessListener<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            Log.i(TAG, "onSuccess: setSwitch is success!");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            if (e instanceof ApiException){
                                Log.e(TAG, "onFailure: There is an error in setSwitch, Code:" + ((ApiException)e).getStatusCode() );
                            }else{
                                Log.e(TAG, "onFailure: There is an error in setSwitch, Message:" + e.getMessage() );
                            }
                        }
                    });
                }
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

    public void submitScoreToLeaderboard() {
        rankingsClient.getRankingSummary(LEADERBOARD_ID, true).addOnSuccessListener(new OnSuccessListener<Ranking>() {
            @Override
            public void onSuccess(Ranking ranking) {
                rankingsClient.submitRankingScore(ranking.getRankingId(), isPlayer1 ? player1Points : player2Points);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException){
                    Log.e(TAG, "onFailure, Code: " + ((ApiException)e).getStatusCode());
                }else {
                    Log.e(TAG, "onFailure: Message: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == resetButton.getId()) {
            if(myReset){
                return;
            }
            myReset = true;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(myReset);
            Data data = Data.fromBytes(stringBuilder.toString().getBytes(Charset.defaultCharset()));
            transferEngine.sendData(mEndPointId, data);
            checkforReset();
            return;
        }

        // if selected button is already colored
        if (!(((Button)v).getText().toString().equals(""))) {
            return;
        }



        // color the tile based on turn
        // TODODONE: do these events with dataTransfer
        if (player1Turn && isPlayer1) {
            ((Button) v).setText("X");
        } else if (!player1Turn && !isPlayer1){
            ((Button) v).setText("O");
        } else {
            return;
        }

        Data data = Data.fromBytes(Integer.toString(v.getId()).getBytes(Charset.defaultCharset()));
        transferEngine.sendData(mEndPointId, data);

        roundCount++;

        if (checkForWin()) {
            if (player1Turn) {
                player1Wins();
                player1Turn = false;
            } else {
                player2Wins();
                player1Turn = true;
            }
        } else if (roundCount == 9) {
            draw();
            player1Turn = !player1Turn;
        } else {
            player1Turn = !player1Turn;
        }
    }

//    private boolean checkForWin() {
//        int [][] field = new int [3][3];
//
//        // get all colors into field array
//        for(int i = 0; i < 3; i++) {
//            for (int j = 0; j < 3 ; j++){
//                field[i][j] = ((ColorDrawable)(buttons[i][j].getBackground())).getColor();
//            }
//        }
//
//        // other 2 for loop(horizontal and vertical) and 2 if(crosswise) just controls if the game is over or not
//        for (int i = 0; i < 3; i++) {
//            if (field[i][0] == field[i][1] && field[i][0] == field[i][2]
//                    && field[i][0] != initialButtonColor) {
//                return true;
//            }
//        }
//
//        for (int i = 0; i < 3; i++) {
//            if (field[0][i] == field[1][i] && field[0][i] == field[2][i]
//                    && field[0][i] != initialButtonColor) {
//                return true;
//            }
//        }
//
//        if (field[0][0] == field[1][1]
//                && field[0][0] == field[2][2]
//                && field[0][0] != initialButtonColor) {
//            return true;
//        }
//
//        if (field[0][2] == field[1][1]
//                && field[0][2] == field[2][0]
//                && field[0][2] != initialButtonColor) {
//            return true;
//        }
//
//        return false;
//
//    }

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

    private void player1Wins() {
        player1Points++;
        submitScoreToLeaderboard();
        // TODO wait the game for 1 second for the points and making visually good
        Toast.makeText(this, "Player 1 wins!", Toast.LENGTH_SHORT).show();
        updatePointsText();
        resetBoard();
    }

    private void player2Wins() {
        player2Points++;
        submitScoreToLeaderboard();
        // TODO wait the game for 1 second for the points and making visually good
        Toast.makeText(this, "Player 2 wins!", Toast.LENGTH_SHORT).show();
        updatePointsText();
        resetBoard();
    }

    private void draw() {
        Toast.makeText(this, "Draw!", Toast.LENGTH_SHORT).show();
        // TODO wait the game for 1 second for the points and making visually good
        resetBoard();
    }

    private void resetBoard() {

        // makes every button with default color (resets their colors to initial colors)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
            }
        }

        roundCount = 0;
    }

    private void updatePointsText() {
        if (myName.compareTo(friendsName) > 0){
            textViewPlayer1.setText(getString(R.string.textViewPlayerRevised, myName, player1Points));
            textViewPlayer2.setText(getString(R.string.textViewPlayerRevised, friendsName, player2Points));
        }else {
            textViewPlayer1.setText(getString(R.string.textViewPlayerRevised, friendsName, player1Points));
            textViewPlayer2.setText(getString(R.string.textViewPlayerRevised, myName, player2Points));
        }

    }

    public class NearbyConnection{

        public static final String TAG = "NearbyConnection";
        public String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        public int PERMISSION_REQUEST_CODE = 7777;
        public DiscoveryEngine mDiscoveryEngine;
        public String packageName;


        public NearbyConnection() {
            if (!checkPerms()) {
                ActivityCompat.requestPermissions(NearbyMulti.this, permissions, PERMISSION_REQUEST_CODE);
            }

            mDiscoveryEngine = Nearby.getDiscoveryEngine(context);

            packageName = context.getPackageName();

        }

        public boolean checkPerms() {
            for(String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "requestPerms: " + permission + " is not granted." );
                    return false;
                }
            }
            return true;
        }

        public void startScanning () {
            ScanOption.Builder scanOption = new ScanOption.Builder();
            scanOption.setPolicy(Policy.POLICY_MESH);
            // 0
            mDiscoveryEngine.startScan(serviceId, scanEndpointCallback, scanOption.build());
        }

        public void startBroadcasting () {
            BroadcastOption.Builder builder = new BroadcastOption.Builder();
            builder.setPolicy(Policy.POLICY_MESH);
            // 0
            mDiscoveryEngine.startBroadcasting(myName, serviceId, connectCallback, builder.build());
        }

        public ScanEndpointCallback scanEndpointCallback = new ScanEndpointCallback() {
            @Override // 1
            public void onFound(String endPointId, ScanEndpointInfo scanEndpointInfo) {
                mEndPointId = endPointId;
                Log.i(TAG, "onFound: " + endPointId + " myNameStr: "
                        + myName + " ScanEndpointInfo: " + scanEndpointInfo.toString());
                mDiscoveryEngine.requestConnect(myName, endPointId, connectCallback);
            }

            @Override
            public void onLost(String s) {

            }
        };

        public ConnectCallback connectCallback = new ConnectCallback() {
            @Override // 2
            public void onEstablish(String endPointId, ConnectInfo connectInfo) {
                mEndPointId = endPointId;
                Log.i(TAG, "onEstablish: " + endPointId + " connectInfo: " + connectInfo.toString());
//            mDiscoveryEngine.acceptConnect(endPointId, dataCallback);
                mDiscoveryEngine.acceptConnect(endPointId, dataCallback);

            }

            @Override // 3
            public void onResult(String endPointId, ConnectResult connectResult) {
                mEndPointId = endPointId;
                Log.i(TAG, "onResult: " + endPointId + " connectResult: " + connectResult.toString());
                transferEngine = Nearby.getTransferEngine(getApplicationContext());
                switch (connectResult.getStatus().getStatusCode()){
                    case StatusCode.STATUS_SUCCESS:
                        mDiscoveryEngine.stopScan();
                        waitLayout.setVisibility(View.GONE);
                        break;
                    case StatusCode.STATUS_CONNECT_REJECTED:
                        Log.e(TAG, "onResult: Status connect Rejected code:" + StatusCode.STATUS_CONNECT_REJECTED );
                        Toast.makeText(context, "One or more player rejected the connection", Toast.LENGTH_LONG).show();
                        destroy();
                        break;
                    default:
                        Log.e(TAG, "onResult: unkown status code: " + connectResult.getStatus().getStatusCode());
                        Toast.makeText(context, "Unknown error. Please re-open", Toast.LENGTH_LONG).show();
                        destroy();
                        break;
                }

//                startGame();
            }

            @Override
            public void onDisconnected(String s) {
                destroy();
            }
        };

        public DataCallback dataCallback = new DataCallback() {

            @Override
            public void onReceived(String s, Data data) {
                // INFO: We need to improve the if checks because onRecieved is executing three times whenever is called.
                Log.i(TAG, "onReceived: s -> " + s + "Data: " + new String(data.asBytes()));
                String receivedData = new String(data.asBytes());

                if (receivedData.contains("true")){
                    if (!friendReset){
                        friendReset = Boolean.parseBoolean(receivedData);
                        checkforReset();
                    }
                    return;
                }

                if(isPlayer1 && !player1Turn && ((Button)(findViewById(Integer.parseInt(receivedData)))).getText().toString().equals("")) {
                    ((Button)(findViewById(Integer.parseInt(receivedData)))).setText("O");
//                            .setBackgroundColor(getResources().getColor(R.color.playerTwo));
                }else if (!isPlayer1 && player1Turn && ((Button)(findViewById(Integer.parseInt(receivedData)))).getText().toString().equals("")) {
                    ((Button)(findViewById(Integer.parseInt(receivedData)))).setText("X");
//                            .setBackgroundColor(getResources().getColor(R.color.playerOne));
                } else {
                    return;
                }
                roundCount++;
                if (checkForWin()) {
                    if (player1Turn) {
                        player1Wins();
                        player1Turn = false;
                    } else {
                        player2Wins();
                        player1Turn = true;
                    }
                } else if (roundCount == 9) {
                    draw();
                    player1Turn = !player1Turn;
                } else {
                    player1Turn = !player1Turn;
                }
            }

            @Override
            public void onTransferUpdate(String s, TransferStateUpdate transferStateUpdate) {

            }
        };

        public void destroy() {
            if (isScanner) {
                mDiscoveryEngine.stopScan();
            } else {
                mDiscoveryEngine.stopBroadcasting();
            }
            mDiscoveryEngine.disconnectAll();
        }
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
}
