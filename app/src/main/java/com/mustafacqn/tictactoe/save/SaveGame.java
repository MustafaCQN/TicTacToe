package com.mustafacqn.tictactoe.save;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.huawei.hms.jos.games.ArchivesClient;
import com.huawei.hms.jos.games.GameScopes;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.archive.Archive;
import com.huawei.hms.jos.games.archive.ArchiveSummary;
import com.huawei.hms.jos.games.archive.OperationResult;
import com.huawei.hms.support.api.entity.auth.Scope;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.result.HuaweiIdAuthResult;
import com.mustafacqn.tictactoe.MainActivity;
import com.mustafacqn.tictactoe.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SaveGame extends Activity implements View.OnClickListener {

    private static final String TAG = "SaveGame";
    public AuthHuaweiId huaweiId;
    public ArchivesClient archivesClient;
    private boolean isRealTime = true;
    public RecyclerView.Adapter adapter;
    public RecyclerView recyclerView;
    public ArrayList<ArchiveSummary> archiveSummaries;
    public Context context;

    private HuaweiIdAuthParams huaweiIdAuthParams;
    private List<Scope> scopes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_game_list);
        context = getApplicationContext();

        scopes = new ArrayList<>();
        scopes.add(GameScopes.DRIVE_APP_DATA);
        huaweiIdAuthParams = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME)
                .setScopeList(scopes).createParams();

        huaweiId = getIntent().getParcelableExtra("hw_account");

        driveSignin();

        recyclerView = findViewById(R.id.save_recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        archiveSummaries = new ArrayList<>();
        adapter = new SaveGameAdapter(archiveSummaries, context, this);
        recyclerView.setAdapter(adapter);

        // 7018 hatasının çözümü
        JosAppsClient appsClient = JosApps.getJosAppsClient(this, huaweiId);
        appsClient.init();

        archivesClient = Games.getArchiveClient(this, huaweiId);
            Task<List<ArchiveSummary>> task = archivesClient.getArchiveSummaryList(isRealTime);
            task.addOnSuccessListener(buffer -> {

            if (buffer != null ) {
                Log.i(TAG, "list size: " + buffer.size());
                // TODO: create an adapter, than bind that list to it to show
                for (ArchiveSummary archiveSummary : buffer) {
                    archiveSummaries.add(archiveSummary);
                }

                recyclerView.setAdapter(adapter);
            }
        }).addOnFailureListener(e -> {
            if (e instanceof ApiException) {
                ApiException apiException = (ApiException) e;
                Log.e(TAG, "onFailure: Status code: " + apiException.getStatusCode());
                if (apiException.getStatusCode() == 7013){
                    if(huaweiId.isExpired()){
                        Log.e(TAG, "onCreate: DOĞRUYMUŞ");
                    }else {
                        Log.e(TAG, "onCreate: YALAN");
                    }
                }
                Toast.makeText(context, "Failed to retrieving the game loads", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    public void onClick(View view) {
        TextView saveName;
        TextView saveDescription;
        TextView savePlayTime;
        if (view instanceof LinearLayout){
            LinearLayout linearLayout = (LinearLayout) view;
            Log.i(TAG, "onClick: child count" + linearLayout.getChildCount());
            if(linearLayout.getChildCount() == 3){
                saveName = (TextView) linearLayout.getChildAt(0);
                saveDescription = (TextView) linearLayout.getChildAt(1);
                savePlayTime = (TextView) linearLayout.getChildAt(2);
            }else {
                Log.e(TAG, "onClick: There is a problem captain! check on click with debug");
                return;
            }
            for (ArchiveSummary archiveSummary : archiveSummaries) {
                if(archiveSummary.getFileName().equals(saveName.getText().toString())
                        && archiveSummary.getDescInfo().equals(saveDescription.getText().toString())
                        && archiveSummary.getActiveTime() == Long.parseLong(savePlayTime.getText().toString())){
                    Task<OperationResult> operationResultTask = archivesClient.loadArchiveDetails(archiveSummary);
                    operationResultTask.addOnSuccessListener(operationResult -> {
                        Archive archive = operationResult.getArchive();
                        if (archive != null && archive.getSummary() != null){
                            Log.i(TAG, "ArchiveId: " + archive.getSummary().getId());
                            try {
                                byte[] data = archive.getDetails().get();
                                // TODO start game with selected data informations (byte to string, than split them with "," to extract the informations that you need)
                                String dataString = new String(data);
                                String [] dataStringArray = dataString.split(",");
                                // 0-> buttons array (XOYOXOY)
                                // 1-> Player1Points(int), 2-> Player2Points(int)
                                // 3-> player1Turn(boolean), 4-> roundCount(int)
                                Intent startGameIntent = new Intent(this, MainActivity.class);
                                startGameIntent.putExtra("buttons",dataStringArray[0]);
                                startGameIntent.putExtra("player_1_points",dataStringArray[1]);
                                startGameIntent.putExtra("player_2_points",dataStringArray[2]);
                                startGameIntent.putExtra("player_1_turn",dataStringArray[3]);
                                startGameIntent.putExtra("round_count",dataStringArray[4]);
                                startGameIntent.putExtras(getIntent()); //hw_account
                                startActivity(startGameIntent);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e(TAG, "onClick: Some error on data parsing with byte");
                            }
                        }else {
                            Log.e(TAG, "onClick: CONFLICT, RUN!!!!!");
                        }
                    }).addOnFailureListener(e -> {
                        if (e instanceof ApiException) {
                            ApiException apiException = (ApiException) e;
                            Log.e("archive", "statusCode:" + apiException.getStatusCode());
                        }
                    });
                }
            }
        }else{
            Log.i(TAG, "onClick: not a linear layout");
        }
    }

    private void driveSignin() {
        Log.i(TAG, "driveSignin: init success");

        Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.getService(this, huaweiIdAuthParams).silentSignIn();
        authHuaweiIdTask.addOnSuccessListener(new OnSuccessListener<AuthHuaweiId>() {
            @Override
            public void onSuccess(AuthHuaweiId authHuaweiId) {
                Log.i(TAG, "onSuccess: signIn success" + authHuaweiId.getDisplayName());
                huaweiId = authHuaweiId;
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
        Intent intent = HuaweiIdAuthManager.getService(this, huaweiIdAuthParams).getSignInIntent();
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
                    huaweiId = signinResult.getHuaweiId();
                    return;
                }else {
                    Log.e(TAG, "onActivityResult: Signin Failed!" + signinResult.getStatus().getStatusCode() );
                }
            }catch (JSONException var){
                Log.e(TAG, "onActivityResult: Failed to convert json");
            } catch (Exception e){
                Log.e(TAG, "onActivityResult: Other Exceptions");
            }
            finish();
        }
    }
}
