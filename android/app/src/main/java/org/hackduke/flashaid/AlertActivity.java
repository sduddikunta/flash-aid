/*
 * This file is part of Flash Aid.
 * Copyright (C) 2014 Siddharth Duddikunta, Steven Zhang, William Yang, Zain Rehmani
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.hackduke.flashaid;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;

public class AlertActivity extends Activity {

    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        int feet = getIntent().getExtras().getInt("feet");
        final double latitude = getIntent().getExtras().getDouble("latitude");
        final double longitude = getIntent().getExtras().getDouble("longitude");
        final String type = getIntent().getExtras().getString("type");

        String typeText = "";
        if (type.equals("general")) {
            typeText = "General Emergency";
        } else if (type.equals("cpr")) {
            typeText = "CPR/AED needed";
        } else if (type.equals("epipen")) {
            typeText = "EpiPen needed";
        }

        ((TextView) findViewById(R.id.emergency_type)).setText(typeText);

        ((TextView) findViewById(R.id.distance_text)).setText(
                String.format(getText(R.string.distance_from_you).toString(), feet));

        findViewById(R.id.navigate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("google.navigation:q=%s,%s",
                        Double.toString(latitude), Double.toString(longitude))));
                startActivity(i);
                finish();
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            Resources res = getResources();
            AssetFileDescriptor afd = res.openRawResourceFd(R.raw.alarm);

            mMediaPlayer = new MediaPlayer();

            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mMediaPlayer.stop();
    }
}
