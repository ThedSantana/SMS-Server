package com.wylder.smsserver;

import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;


/**
 * This class displays the settings for the server on the device
 */
public class SettingsActivity extends PreferenceActivity {

    private Preference url;
    private EditTextPreference port;
    private EditTextPreference password;
    private SwitchPreference masterSwitch;
    private WifiManager manager;

    /**
     * First function called when the app starts. We use this to setup preferences and construct things
     * @param sis a bundle of data full of info about the last instance... but It's empty because we don't need it
     */
    @Override
    public void onCreate(Bundle sis){
        super.onCreate(sis);
        addPreferencesFromResource(R.xml.preferences);
        url = findPreference("url");
        password = (EditTextPreference) findPreference("password");
        port = (EditTextPreference) findPreference("port");
        masterSwitch = (SwitchPreference) findPreference("server_on");
        manager = (WifiManager) this.getSystemService(WIFI_SERVICE);
        bindListener("port");
        bindListener("server_on");

        // create an actionbar because this is a PreferenceActivity
        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setOnClickListener(new View.OnClickListener() {
            int counter = 0;
            int numberOfTaps = 6;
            @Override
            public void onClick(View view) {
                // if the user taps the actionbar 6 times launch a secret activity
                if(++counter >= 3 && counter != numberOfTaps){
                    Toast.makeText(SettingsActivity.this, "Touch " + (numberOfTaps - counter) + " more times to start secret options", Toast.LENGTH_LONG).show();
                }else if(counter == numberOfTaps){
                    counter = 0;
                    // launch secret activity here
                }
            }
        });

        // setup the help button
        findPreference("help").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, HelpActivity.class);
                startActivity(intent);
                return false;
            }
        });
    }

    /**
     * This object responds to the bound preferences' changes
     */
    private Preference.OnPreferenceChangeListener preferenceChange = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            if(preference instanceof EditTextPreference){
                // port edit text preference
                preference.setSummary(o.toString());
                setIp();
            }else if(preference instanceof SwitchPreference){
                boolean on = (boolean) o;
                if(preference.getKey().equals("server_on")){
                    // server on switch
                    Intent intent = new Intent(SettingsActivity.this, WebService.class);
                    if(on){
                        port.setEnabled(false); // user can't change the port now
                        startService(intent);
                    }else{
                        stopService(intent);
                        port.setEnabled(true);  // user can now edit the server's port number b/c the server isn't running
                    }
                }
            }
            return true;
        }
    };

    /**
     * This will listen for when/if the network drops out
     */
    private ConnectionLogic.OnNetworkChangeListener networkListener = new ConnectionLogic.OnNetworkChangeListener() {
        @Override
        public void onChange(boolean connection) {
            setIp();
            masterSwitch.setEnabled(connection);
            if(!connection){
                masterSwitch.setChecked(false);
                port.setEnabled(true);
            }
        }
    };

    /**
     * a helper method to set the text field to the address of our server
     */
    private void setIp(){
        // sets the url field to the ip address:port
        int ipAddress = manager.getConnectionInfo().getIpAddress();
        final String ip = String.format("http://%d.%d.%d.%d:%s", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff), port.getText());
        WifiInfo connectionInfo = manager.getConnectionInfo();
        if(ConnectionLogic.isConnected(this)){
            url.setSummary(ip);
            // make a share intent to open the app on click
            url.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent share = new Intent(android.content.Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_SUBJECT, "SMS Server local url");
                    share.putExtra(Intent.EXTRA_TEXT, ip);
                    startActivity(Intent.createChooser(share, "Share Link"));
                    return false;
                }
            });
        }else{
            url.setSummary("WiFi not connected");
            // remove the share option
            url.setOnPreferenceChangeListener(null);
        }
    }

    /**
     * this binds a preference to the change listener and triggers it
     * @param name the string key of the preference
     */
    private void bindListener(String name) {
        // Set the listener to watch for value changes.
        Preference preference = findPreference(name);
        preference.setOnPreferenceChangeListener(preferenceChange);
        // trigger the listener with initial values
        if(preference instanceof EditTextPreference) {
            preferenceChange.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(name, ""));
        }else if(preference instanceof SwitchPreference){
            preferenceChange.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(name, false));
        }
    }

    @Override
    public void onPause(){
        ConnectionLogic.removeOnNetworkChangeListener(networkListener);
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        ConnectionLogic.addOnNetworkChangeListener(networkListener, this);

        // set the master switch to the correct value
        masterSwitch.setEnabled(ConnectionLogic.isConnected(this));
    }
}
