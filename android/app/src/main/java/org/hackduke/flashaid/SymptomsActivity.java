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

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

public class SymptomsActivity extends BaseActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private boolean hasCalled;
    private LocationClient mLocationClient;
    private Location mLocation;
    private CountDownTimer mTimer;
    private TextToSpeech mTTS;
    private boolean mTTSInitComplete = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptoms);
        final String stringToShow = ((TextView) findViewById(R.id.time_left)).getText().toString();
        mTimer = new CountDownTimer(16000, 1000) {

            public void onTick(long millisecondsLeft) {
                int intTimeLeft = (int) ((millisecondsLeft / 1000));
                ((TextView) findViewById(R.id.time_left)).setText(String.format(stringToShow, intTimeLeft));
            }

            public void onFinish() {
                if (!hasCalled) {
                    ((TextView) findViewById(R.id.time_left)).setText("Calling...");
                    onSendCallButtonClick();
                }
            }
        };
        mTimer.start();
        findViewById(R.id.send_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((TextView) findViewById(R.id.time_left)).setText("Calling...");
                hasCalled = true;
                onSendCallButtonClick();
            }
        });
        mLocationClient = new LocationClient(this, this, this);

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                mTTSInitComplete = true;
                mTTS.setSpeechRate(0.9f);
                mTTS.speak("What's your emergency?", TextToSpeech.QUEUE_FLUSH, null);
                mTTS.speak("A call will still be sent even if you do not respond.", TextToSpeech.QUEUE_ADD,
                        null);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationClient.disconnect();
        mTimer.cancel();
        if (mTTSInitComplete) {
            mTTS.stop();
            mTTS.shutdown();
        }
    }

    private void onSendCallButtonClick() {
        if (mLocation == null) {
            try {
                synchronized (this) {
                    wait(15000);
                }
            } catch (InterruptedException ex) {
                // who cares
            }
        }
        if (mLocation != null) {
            RequestQueue queue = Network.getRequestQueue(getApplicationContext());
            String url = Network.API_ENDPOINT + "/alerts";
            final String email = getSharedPreferences("user", MODE_PRIVATE).getString("email", "");
            String type = "";
            if (((RadioButton) findViewById(R.id.radio_general)).isChecked()) {
                type = "general";
            } else if (((RadioButton) findViewById(R.id.radio_cpr)).isChecked()) {
                type = "cpr";
            } else if (((RadioButton) findViewById(R.id.radio_epi)).isChecked()) {
                type = "epipen";
            }
            final String theRealType = type;
            StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String s) {
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    // TODO: handle error
                }
            }) {
                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return String.format("{\"email\":\"%s\",\"latitude\":\"%s\",\"longitude\":\"%s\",\"type\":\"%s\"}",
                                email, mLocation.getLatitude(), mLocation.getLongitude(), theRealType).getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        return new byte[]{};
                    }
                }

                @Override
                public String getBodyContentType() {
                    return "application/json";
                }
            };
            queue.add(request);
        }

        if (mTTSInitComplete) {
            mTTS.speak("Help is on the way.", TextToSpeech.QUEUE_FLUSH, null);
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                String number = "6786710286";
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + number));
                startActivity(intent);
                finish();
            }
        }, 2000);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocation = mLocationClient.getLastLocation();
        mLocationClient.disconnect();
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void onDisconnected() {
        // TODO: I DONT CARE
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO: I DONT CARE
    }
}
