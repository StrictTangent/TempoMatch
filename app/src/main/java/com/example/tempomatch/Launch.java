package com.example.tempomatch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

public class Launch extends AppCompatActivity {

    Switch savedTracksSwitch;
    Switch savedPlaylistsSwitch;
    Switch categorySwitch;
    Switch predefinedSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        savedTracksSwitch = findViewById(R.id.savedTracksSwitch);
        savedPlaylistsSwitch = findViewById(R.id.savedPlaylistsSwitch);
        categorySwitch = findViewById(R.id.categorySwitch);
        predefinedSwitch = findViewById(R.id.predefinedSwitch);

    }

    public void launchMain(View view) {

        boolean[] parameters = new boolean[4];

        if (savedTracksSwitch.isChecked()) parameters[0] = true;
        if (savedPlaylistsSwitch.isChecked()) parameters[1] = true;
        if (categorySwitch.isChecked()) parameters[2] = true;
        if (predefinedSwitch.isChecked()) parameters[3] = true;

        boolean validSelection = false;
        for (int i = 0; i < parameters.length; i++){
            if (parameters[i]) validSelection = true;
        }

        if (validSelection) {

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("parameters", parameters);
            finish();
            startActivity(intent);
        } else {
            Toast.makeText(this, "MUST CHOOSE AT LEAST ONE SOURCE", Toast.LENGTH_LONG).show();
        }
    }
}
