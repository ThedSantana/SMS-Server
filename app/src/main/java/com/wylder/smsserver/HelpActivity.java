package com.wylder.smsserver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.lang.annotation.Annotation;

/**
 * Created by kevin on 5/7/15.
 */
public class HelpActivity extends Activity {

    @Override
    public void onCreate(Bundle sis){
        super.onCreate(sis);
        WebView view = new WebView(this);
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        view.loadUrl("file:///android_res/raw/about.html");
        view.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                showLicenseDialog();
                result.cancel();
                return true;
            }
        });
        setContentView(view);
    }

    private void showLicenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SMS Server License");
        builder.setMessage("SMS Server uses NanoHTTPD, which is distributed under a Modified BSD license. For more info, see https://github.com/NanoHttpd/nanohttpd");
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Visit License", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/NanoHttpd/nanohttpd"));
                startActivity(intent);
            }
        });
        builder.create().show();
    }

}
