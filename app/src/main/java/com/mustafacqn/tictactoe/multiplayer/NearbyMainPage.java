package com.mustafacqn.tictactoe.multiplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.mustafacqn.tictactoe.R;

public class NearbyMainPage extends Activity {

    EditText myName;
    EditText friendsName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multiplayer_connection);

        myName = findViewById(R.id.yourNameET);
        friendsName = findViewById(R.id.friendsNameET);
    }

    public void startConnection (View v){
        Intent intent = new Intent(this, NearbyMulti.class);
        intent.putExtra("myname", myName.getText().toString());
        intent.putExtra("friendsname", friendsName.getText().toString());
        intent.putExtra("hw_account", (AuthHuaweiId) getIntent().getParcelableExtra("hw_account"));
        startActivity(intent);
    }
}
