package com.wylder.smsserver;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by kevin on 4/25/15.
 */
public class WebService extends Service {

    SharedPreferences preferences;
    WylderServer server;

    /**
     * This listener will be registered to fire when the connection changes
     */
    ConnectionLogic.OnNetworkChangeListener networkListener = new ConnectionLogic.OnNetworkChangeListener() {
        @Override
        public void onChange(boolean connection) {
            if(!connection){
                Log.e("KevinRuntime", "Shut down no wifi");
                stopSelf();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    /**
     * This is where the service is setup. We will instantiate SharedPreferences, start the web server,
     * and start a few broadcast receivers.
     */
    @Override
    public void onCreate(){
        // instantiate SharedPreferences
        preferences = this.getSharedPreferences("com.wylder.smsserver_preferences", Context.MODE_PRIVATE);
        preferences.edit().putBoolean("server_on", true).apply();
        // start Server
        try {
            server = new WylderServer();
            server.start();
            Toast.makeText(this, "SMS Server Activated", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Socket in use", Toast.LENGTH_LONG).show();
            preferences.edit().putBoolean("server_on", false).apply();
            stopSelf();
        }
        // add the network listener to handle changes
        ConnectionLogic.addOnNetworkChangeListener(networkListener, this);
    }

    /**
     * Here we clean up the service.
     */
    @Override
    public void onDestroy(){
        preferences.edit().putBoolean("server_on", false).apply();
        Toast.makeText(this, "SMS Server Deactivated", Toast.LENGTH_SHORT).show();
        server.parser.wrapper.releaseWrapper();     // this feels dirty
        server.stop();
        // prevent the networkListener from firing after the server is stopped
        ConnectionLogic.removeOnNetworkChangeListener(networkListener);
    }

    /**
     * This is the class that contains the server info. We extend the already huge fi.iki.elonen.NanoHTTPD
     */
    protected class WylderServer extends NanoHTTPD {

        HTMLParser parser = new HTMLParser(WebService.this);
        public int token = 0;

        public WylderServer() throws IOException {
            // start server with a given port number saved in SharedPreferences
            super(Integer.parseInt(preferences.getString("port", "3446")));
        }

        /**
         * This is the main serving method for the web server. If a request is sent to the phone, all
         * it's headers and info are available here
         * @param session The HTTP session
         * @return the response to send back to the browser
         */
        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            try {
                session.parseBody(null);
            }catch(Exception e){
                e.printStackTrace();
            }
            Map<String, String> post = session.getParms();
            boolean hasToken = false;
            boolean hasPassword = false;
            if(post.get("token") != null) {
                hasToken = post.get("token").equals("" + token);
            }
            if(post.get("pwd") != null) {
                hasPassword = post.get("pwd").equals(preferences.getString("password", ""));
            }
            String number = post.get("number");
            String message = post.get("message");
            if(uri.equals("/")){
                // welcome page
                return parser.getLoginPage(false);
            }else if(uri.equals("/home") && hasPassword){
                // home page
                token = (int)(Math.random() * Integer.MAX_VALUE);
                return parser.getTextingPage(token);
            }else if(uri.equals("/poll") && hasToken){
                // polling
                return parser.getNotificationPoll();
            }else if(uri.equals("/getthread") && hasToken){
                // get a text thread
                return parser.getConversation(number);
            }else if(uri.equals("/inbox") && hasToken) {
                // get the recent contacts
                return parser.getInbox();
            }else if(uri.equals("/send") && hasToken){
                // send text
                parser.wrapper.sendMessage(number, message);
                return new Response("");
            }else{
                return parser.getLoginPage(true);
            }

        }

    }

}
