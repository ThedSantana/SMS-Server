package com.wylder.smsserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kevin on 4/25/15.
 */
public class TextingWrapper {

    private Context context;
    private boolean requestUpdate = false;
    private BroadcastReceiver smsReceiver;

    /**
     * This constructor creates a broadcastreciever to listen for incoming texts.
     * @param ctx the context of the service holding the wrapper.
     */
    public TextingWrapper(Context ctx){
        context = ctx;
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                requestUpdate = true;
                Log.e("KevinRuntime", "Text received");
            }
        };
        context.registerReceiver(smsReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
    }

    /**
     * once done with this wrapper, release it so it can give up the broadcastreciever
     */
    public void releaseWrapper(){
        context.unregisterReceiver(smsReceiver);
    }

    /**
     * retrieves a list of contacts in alphabetical order as a JSONArray. each element is a JSON object
     * with parameters: name number.
     * @return a JSONArray of contacts
     * @throws JSONException if something goes terribly wrong
     */
    public JSONArray getContactList() throws JSONException{
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY
        );
        int nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int numCol  = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        JSONArray array = new JSONArray();
        while(cursor.moveToNext()){
            JSONObject contact = new JSONObject();
            contact.put("name", cursor.getString(nameCol));
            contact.put("number", cursor.getString(numCol));
            array.put(contact);
        }
        cursor.close();
        Log.d("KevinDebug", "Found Contacts list:");
        Log.d("KevinDebug", array.toString());
        return array;
    }

    /**
     * This method gets the contacts most recently texted people
     * @return A json array with the contact's names ONLY
     * @throws JSONException shouldn't ever be called
     */
    public JSONArray getRecentsList() throws JSONException{
        Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS},
                null,
                null,
                "MAX(" + Telephony.Sms.DATE + ", " + Telephony.Sms.DATE_SENT + ") DESC"
        );
        JSONArray array = new JSONArray();
        ArrayList<String> addresses = new ArrayList<>();
        while(cursor.moveToNext() && addresses.size() < 5) {
            String person = stripNumber(cursor.getString(0));
            if (!addresses.contains(person)) {
                addresses.add(person);
                array.put(person);
            }
        }
        cursor.close();
        Log.d("KevinDebug", "Found Recents list:");
        Log.d("KevinDebug", array.toString());
        return array;
    }

    /**
     * This gets the whole texting conversation of a given phone number. The input is cleaned of normal
     * phone number formatting.
     * @param phoneNumber
     * @return A json array of objects that have a message and sender.
     * @throws JSONException
     */
    public JSONArray getTextThread(String phoneNumber) throws JSONException {
        String prettyNumber = stripNumber(phoneNumber);
        Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE},
                Telephony.Sms.ADDRESS + " LIKE ?",
                new String[]{"%" + prettyNumber},
                "date ASC"
        );
        JSONArray array = new JSONArray();
        while(cursor.moveToNext()){
            JSONObject message = new JSONObject();
            message.put("message", cursor.getString(1));
            message.put("sender", cursor.getInt(3));
            array.put(message);
        }
        cursor.close();
        Log.d("KevinDebug", "Found Text thread from " + phoneNumber + " which was truncated to " + prettyNumber);
        Log.d("KevinDebug", array.toString());
        return array;
    }

    /**
     * Get the most frequently texted contacts in the last month. Only the Number is returned in an
     * array because the web page can search for the name in the contacts list.
     * @return
     * @throws JSONException
     */
    public JSONArray getFrequentContacts() throws JSONException {
        Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.Conversations.CONTENT_URI,
                new String[]{Telephony.Sms.Conversations.THREAD_ID},
                null,
                null,
                Telephony.Sms.Conversations.MESSAGE_COUNT + " DESC LIMIT 10"
        );
        JSONArray array = new JSONArray();
        ArrayList<String> addresses = new ArrayList<>();
        while(cursor.moveToNext() && addresses.size() < 5) {
            String threadId = cursor.getString(0);
            Cursor cursor2 = context.getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    new String[]{"DISTINCT " + Telephony.Sms.ADDRESS},
                    Telephony.Sms.THREAD_ID + "=?",
                    new String[]{threadId},
                    Telephony.Sms.ADDRESS + " DESC LIMIT 2"
            );
            cursor2.moveToFirst();
            String person = stripNumber(cursor2.getString(0));
            if (!addresses.contains(person)) {
                addresses.add(person);
                array.put(person);
            }
            cursor2.close();
        }
        cursor.close();
        Log.d("KevinDebug", "Found Frequent Contacts list:");
        Log.d("KevinDebug", array.toString());
        return array;
    }

    /**
     * Gets a json encodable boolean that determines if the web client needs an update
     * @return a string, "true" or "false" to be returned for a NanoHTTPD.Response
     */
    public String newMessage(){
        if(requestUpdate) {
            requestUpdate = false;
            return "true";
        }else
            return "false";
    }

    /**
     * sends an SMS message to the given string address (phone number)
     * @param address a string phone number to send to. All ()+- and whitespace will be trimmed
     * @param message the text to send. It will be split into multiple texts if too long.
     */
    public void sendMessage(String address, String message){
        SmsManager manager = SmsManager.getDefault();
        ArrayList<String> messageDivided = manager.divideMessage(message);
        manager.sendMultipartTextMessage(address, null, messageDivided, null, null);
        Log.d("KevinDebug", "Sending text '" + message + "' to address " + address);
        requestUpdate = true;
    }

    /**
     * helper method to strip a phone number of ()+- and whitespace
     */
    public static String stripNumber(String input){
        return input.replaceAll("[() -+]", "");
    }

}
