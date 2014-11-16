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

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationClient;

public class GcmIntentService extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private LocationClient mLocationClient;
    private Location mLocation;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mLocationClient = new LocationClient(this, this, this);
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                System.out.println(extras.toString());
                mLocationClient.connect();
                try {
                    synchronized (this) {
                        wait(15000);
                    }
                } catch (InterruptedException ex) {
                    // continue
                }
                mLocationClient.disconnect();
                if (mLocation != null) {
                    double lat = Double.valueOf(extras.getString("latitude", "0.0"));
                    double lon = Double.valueOf(extras.getString("longitude", "0.0"));
                    if (Math.abs(distanceBetweenPoints(mLocation.getLatitude(), mLocation.getLongitude(), lat, lon))
                            < 1) {
                        Log.i("GcmIntentService", "ALERT!");
                        Bundle arguments = new Bundle();
                        arguments.putInt("feet", (int) Math.abs(distanceBetweenPoints(mLocation.getLatitude(),
                                mLocation.getLongitude(), lat, lon) * 5280));
                        arguments.putDouble("latitude", lat);
                        arguments.putDouble("longitude", lon);
                        arguments.putString("type", extras.getString("type"));
                        Intent activity = new Intent(this, AlertActivity.class);
                        activity.putExtras(arguments);
                        activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(activity);
                    }
                }
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocation = mLocationClient.getLastLocation();
        Log.i("GcmIntentService", "Got a location");
        synchronized (this) {
            notifyAll();
        }
        mLocationClient.disconnect();
    }

    @Override
    public void onDisconnected() {
        // TODO: what is error handling?
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO: what is error handling?
    }

    private double distanceBetweenPoints(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Radius of the earth in km
        double dLat = deg2rad(lat2 - lat1);  // deg2rad below
        double dLon = deg2rad(lon2 - lon1);
        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c; // Distance in km
        return d * 0.621371; // distance in mi
    }

    private double deg2rad(double deg) {
        return deg * (Math.PI / 180);
    }
}