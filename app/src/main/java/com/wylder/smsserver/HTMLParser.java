package com.wylder.smsserver;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by kevin on 4/25/15.
 */
public class HTMLParser {

    TextingWrapper wrapper;
    Context ctx;

    public HTMLParser(Context ctx){
        wrapper = new TextingWrapper(ctx);
        this.ctx = ctx;
    }

    public NanoHTTPD.Response getTextingPage(int token){
        HashMap<String, String> map = new HashMap<>();
        try {
            map.put("ADDRESS_BOOK", wrapper.getContactList().toString());
            map.put("FREQUENT_CONTACTS", wrapper.getFrequentContacts().toString());
            map.put("TOKEN", "" + token);
        }catch (Exception e){
            Log.e("KevinRuntime", "there was an error parsing json");
            e.printStackTrace();
        }
        return new NanoHTTPD.Response(parse(R.raw.texting, map));
    }

    public NanoHTTPD.Response getConversation(String conversationId){
        try {
            return new NanoHTTPD.Response(wrapper.getTextThread(conversationId).toString());
        }catch (Exception e){
            e.printStackTrace();
            return new NanoHTTPD.Response("[{\"message\":\"" + e.getMessage() + "\", \"sender\":0}]");
        }
    }

    public NanoHTTPD.Response getLoginPage(boolean error){
        HashMap<String, String> map = new HashMap<>();
        String display = error ? "block" : "none";
        map.put("ERROR", display);
        map.put("PHONE_NAME", Build.MODEL);
        return new NanoHTTPD.Response(parse(R.raw.login, map));
    }

    public NanoHTTPD.Response getNotificationPoll(){
        return new NanoHTTPD.Response(wrapper.newMessage());
    }

    public NanoHTTPD.Response getInbox(){
        try {
            return new NanoHTTPD.Response(wrapper.getRecentsList().toString());
        }catch(JSONException e){
            e.printStackTrace();
            return new NanoHTTPD.Response("[{\"name\":\"Kevin Wylder\" \"number\":\"your code sucks\"}]");
        }
    }

    /**
     * This is a really shitty implementation of parsing the given resource with the injections.
     * @param resourceId the id of the resource to parse in R.raw
     * @param injections a list of fields to inject into with appropriate values
     * @return the whole HTML page as a string (this hurts to write... plz optimize later)
     */
    private String parse(int resourceId, Map<String, String> injections){
        InputStreamReader inputStream = new InputStreamReader(ctx.getResources().openRawResource(resourceId));
        StringBuilder builder = new StringBuilder();
        try {
            int read = inputStream.read();
            while (read != -1) {  // iterate till the end of the file, char by char
                // if the next byte read out of the stream is a <
                if(read == (int)'<'){
                    // read another one. if its a ?, we'll start to replace
                    read = inputStream.read();
                    if(read == (int)'?'){
                        // WE'RE REPLACING
                        // read the stuff inside <?> and put it in replaceId
                        StringBuilder replaceId = new StringBuilder();
                        read = inputStream.read();
                        while(read != (int)'>' && read != -1){
                            replaceId.append((char) read);
                            read = inputStream.read();
                        }
                        String id = replaceId.toString();
                        // add the key to the file
                        builder.append(injections.get(id));

                    }else{
                        // False alarm
                        builder.append('<');
                        builder.append((char) read);
                    }
                }else{
                    // the default behavior. just add the char to the string
                    builder.append((char) read);
                }
                read = inputStream.read();
            }
        }catch(IOException exception){
            exception.printStackTrace();
            return "IOException... " + exception.getMessage();
        }
        return builder.toString();
    }

}
