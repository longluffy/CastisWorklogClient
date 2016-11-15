package com.castis.castisworklogclient.Presenter;

import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class HttpHandler {

    private static final String TAG = HttpHandler.class.getSimpleName();

    public HttpHandler() {
    }

    public InputStream requestAPI(String reqUrl,Object inputobj) {
        Gson gsonParser = new Gson();
        InputStream inputStream = null;
        try {
            String json = gsonParser.toJson(inputobj);

            HttpURLConnection urlConnection = null;
            URL apiUrl = new URL(reqUrl);
            urlConnection = (HttpURLConnection) apiUrl.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            Writer writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
            Log.i(TAG, "long dang debug");
            writer.write(json);
            System.out.println(json);
            writer.close();
            System.out.println("den day roi");
            inputStream = urlConnection.getInputStream();
            System.out.println(inputStream);
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException: " + e.getMessage());
        } catch (ProtocolException e) {
            Log.e(TAG, "ProtocolException: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
        return inputStream;
    }

}
