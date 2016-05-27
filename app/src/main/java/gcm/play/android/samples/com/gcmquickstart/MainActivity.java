/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gcm.play.android.samples.com.gcmquickstart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import gcm.play.android.samples.com.gcmquickstart.Models.messageInfo;

public class MainActivity extends AppCompatActivity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";
    public static final String API_KEY = "AIzaSyDnaKW_-UoCIlsT9in5PcVADXDJ1Jtr8sg";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private BroadcastReceiver mMessageReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    mInformationTextView.setText(getString(R.string.gcm_send_message));
                } else {
                    mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };
        mInformationTextView = (TextView) findViewById(R.id.informationTextView);

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                String message = intent.getStringExtra("msg");
                Log.d("receiver", "Got message: " + message);

                // Save message to DB
                messageInfo mi = new messageInfo();
                mi.setText(message);
                DataController.getInstance(MainActivity.this).saveMessage(mi);

                // Ass message to list
                adapter.add(mi.getText());
                adapter.notifyDataSetChanged();
            }
        };
        TextView tvMsg = (TextView) findViewById(R.id.message);
        final String msg = tvMsg.getText().toString();
        Button btnSendMessage = (Button) findViewById(R.id.sendMessage);
        if (btnSendMessage != null) {
            btnSendMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {

                                if (msg.length() != 0) {
                                    JSONObject jGcmData = new JSONObject();
                                    JSONObject jData = new JSONObject();
                                    jData.put("message", msg);

                                    jGcmData.put("to", "/topics/global");
                                    jGcmData.put("data", jData);

                                    URL url = new URL("https://android.googleapis.com/gcm/send");
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setRequestProperty("Authorization", "key=" + API_KEY);
                                    conn.setRequestProperty("Content-Type", "application/json");
                                    conn.setRequestMethod("POST");
                                    conn.setDoOutput(true);

                                    OutputStream outputStream = conn.getOutputStream();
                                    outputStream.write(jGcmData.toString().getBytes());

                                    InputStream inputStream = conn.getInputStream();
                                    String resp = IOUtils.toString(inputStream);
                                    System.out.println(resp);
                                    System.out.println("Check your device/emulator for notification or logcat for " + "confirmation of the receipt of the GCM message.");
                                }
                            } catch (IOException e) {
                                System.out.println("Unable to send GCM message.");
                                System.out.println("Please ensure that API_KEY has been replaced by the server " + "API key, and that the device's registration token is correct (if specified).");
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    };
                    task.execute();
                }
            });
        }

        // Message history
        ArrayList<messageInfo> results = DataController.getInstance(MainActivity.this).getMessages();
        ArrayList<String> items = new ArrayList<>();
        for (messageInfo item : results) {
            items.add(item.getText());
        }

        ListView lv = (ListView) findViewById(R.id.messagesList);
        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, items);
        lv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver, new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
        registerReceiver(mMessageReceiver,
                new IntentFilter("customEvent"));

        //LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("customEvent"));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

}
