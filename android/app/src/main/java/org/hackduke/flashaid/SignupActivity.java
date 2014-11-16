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
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class SignupActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        TelephonyManager tMgr = (TelephonyManager)
                getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tMgr.getLine1Number();

        ((EditText) findViewById(R.id.phone)).setText(mPhoneNumber);

        findViewById(R.id.register_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRegisterButtonClicked();
            }
        });

        getLoaderManager().initLoader(0, null, this);

        ((Toolbar) findViewById(R.id.toolbar)).setTitle(getTitle());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arguments) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(
                        ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
                ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE + " = ?",
                new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            // Potentially filter on ProfileQuery.IS_PRIMARY
            cursor.moveToNext();
        }
        if (emails.size() > 0) {
            ((EditText) findViewById(R.id.email)).setText(emails.get(0));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    private void onRegisterButtonClicked() {
        final String name = ((EditText) findViewById(R.id.name)).getText().toString();
        final String email = ((EditText) findViewById(R.id.email)).getText().toString();
        final String phone = ((EditText) findViewById(R.id.phone)).getText().toString();

        boolean hasError = false;

        if (name.isEmpty()) {
            ((EditText) findViewById(R.id.name)).setError(getText(R.string.name_empty));
            hasError = true;
        } else {
            ((EditText) findViewById(R.id.name)).setError(null);
        }
        if (!email.contains("@")) {
            ((EditText) findViewById(R.id.email)).setError(getText(R.string.email_invalid));
            hasError = true;
        } else {
            ((EditText) findViewById(R.id.email)).setError(null);
        }
        if (!PhoneNumberUtils.isGlobalPhoneNumber(phone)) {
            ((EditText) findViewById(R.id.phone)).setError(getText(R.string.phone_invalid));
            hasError = true;
        } else {
            ((EditText) findViewById(R.id.phone)).setError(null);
        }

        if (!hasError) {
            findViewById(R.id.register_button).setEnabled(false);
            findViewById(R.id.progress).setVisibility(View.VISIBLE);

            RequestQueue queue = Network.getRequestQueue(getApplicationContext());
            String url = Network.API_ENDPOINT + "/users";

            StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String s) {
                    getSharedPreferences("user", MODE_PRIVATE).edit().putString("email", email).apply();

                    Intent intent = new Intent(SignupActivity.this, CompleteProfileActivity.class);
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
                        return String.format("{\"name\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\"}",
                                name, email, phone).getBytes("UTF-8");
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

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }
}
