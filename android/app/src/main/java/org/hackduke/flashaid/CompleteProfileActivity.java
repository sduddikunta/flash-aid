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
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.io.UnsupportedEncodingException;

public class CompleteProfileActivity extends ActionBarActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);
        findViewById(R.id.done_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDoneButtonClick();
            }
        });

        ((Toolbar) findViewById(R.id.toolbar)).setTitle(getTitle());
    }

    private void onDoneButtonClick() {
        final boolean isFirstResponder = ((RadioButton) findViewById(R.id.radio_yes)).isChecked();
        final boolean isCpr = ((CheckBox) findViewById(R.id.checkbox_cpr)).isChecked();
        final boolean isAed = ((CheckBox) findViewById(R.id.checkbox_aed)).isChecked();
        final boolean isEpi = ((CheckBox) findViewById(R.id.checkbox_epipen)).isChecked();
        final String firstEmergencyContactInfoName = ((EditText) findViewById(R.id.person_one_name)).getText().toString();
        final String firstEmergencyContactInfoNumber = ((EditText) findViewById(R.id.person_one_number)).getText().toString();
        final String secondEmergencyContactInfoName = ((EditText) findViewById(R.id.person_two_name)).getText().toString();
        final String secondEmergencyContactInfoNumber = ((EditText) findViewById(R.id.person_two_number)).getText().toString();

        boolean hasError = false;

        if (firstEmergencyContactInfoName.isEmpty()) {
            ((EditText) findViewById(R.id.person_one_name)).setError(getText(R.string.name_empty));
            hasError = true;
        } else {
            ((EditText) findViewById(R.id.person_one_name)).setError(null);
        }
        if (!PhoneNumberUtils.isGlobalPhoneNumber(firstEmergencyContactInfoNumber)) {
            ((EditText) findViewById(R.id.person_one_number)).setError(getText(R.string.phone_invalid));
            hasError = true;
        } else {
            ((EditText) findViewById(R.id.person_one_number)).setError(null);
        }

        final String email = getSharedPreferences("user", MODE_PRIVATE).getString("email", "");

        if (!hasError) {
            findViewById(R.id.done_button).setEnabled(false);
            findViewById(R.id.progress).setVisibility(View.VISIBLE);

            RequestQueue queue = Network.getRequestQueue(getApplicationContext());
            String url = Network.API_ENDPOINT + "/user/profile";

            StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String s) {
                    getSharedPreferences("user", MODE_PRIVATE).edit().putBoolean("has_profiled", true).apply();

                    Intent intent = new Intent(CompleteProfileActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
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
                        return String.format("{\"first_responder\":%s,\"cpr\":%s,\"aed\":%s," +
                                        "\"epipen\":%s,\"emergency_1_name\":\"%s\"," +
                                        "\"emergency_1_phone\":\"%s\",\"emergency_2_name\":\"%s\"," +
                                        "\"emergency_2_phone\":\"%s\",\"email\":\"%s\"}",
                                isFirstResponder ? "true" : "false",
                                isCpr ? "true" : "false",
                                isAed ? "true" : "false",
                                isEpi ? "true" : "false",
                                firstEmergencyContactInfoName,
                                firstEmergencyContactInfoNumber,
                                secondEmergencyContactInfoName,
                                secondEmergencyContactInfoNumber,
                                email).getBytes("UTF-8");
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
    }

    public void onRadioButtonClicked(View v) {
        if (((RadioButton) findViewById(R.id.radio_yes)).isChecked()) {
            findViewById(R.id.checkboxen).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.checkboxen).setVisibility(View.GONE);
        }
    }
}
