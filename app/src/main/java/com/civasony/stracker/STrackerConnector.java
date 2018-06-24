package com.civasony.stracker;


import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ViewDebug;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by 5109U11454 on 2018/6/9.
 */

//
class STrackerConnector extends Thread {
    private String ip = null;
    private int bufferSize = 1024;
    private Handler mainHandler = null;
    public Handler postHandler = null;
    private String strUrlPath = null;
    private URL url = null;
    private HttpURLConnection conn = null;
    private OutputStream out = null;
    private InputStream in = null;

    STrackerConnector(Handler mainHandler){
        this.mainHandler = mainHandler;
        setIp("192.168.3.62");
    }

    public void setIp(String ip) {
        this.ip = ip;
        strUrlPath = "http://" + ip + ":8080/image/?timeout=3";
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void run() {
        super.run();
        Looper.prepare();
        postHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                sendToServer(1,(byte[])msg.obj);
                int information = receiveFromServer();
            }
        };
        Looper.loop();
    }

    private boolean sendToServer(int fileId, byte[] imageBuf){
        try {
            url = new URL(strUrlPath);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true); //如果要输出，则必须加上此句
            out = conn.getOutputStream();

            DataInputStream d = new DataInputStream(new ByteArrayInputStream(imageBuf));
            byte[] tempBuffer = new byte[bufferSize];
            while(true){
                int len = d.read(tempBuffer);
                if (len > 0)
                    out.write(tempBuffer);
                else
                    break;
            }
            out.close();
            return true;
        }catch (Exception e){
            Log.d("PostThread err: ", e.getLocalizedMessage());
            return false;
        }
    }

    private int receiveFromServer(){
        // received server message type json:{"id":integerValue}
        int resultID = -1;
        try {
            if (conn.getResponseCode() == 200) {

                String content = "";
                in = conn.getInputStream();
                InputStreamReader isr = new InputStreamReader(in,"utf-8");
                BufferedReader reader = new BufferedReader(isr);
                String line = null;
                while((line = reader.readLine()) != null){
                    content += line;
                }
                sendToMainHandler(1, content);
                in.close();
                conn.disconnect();
            }
        }catch (Exception e){
            Log.d("getResponseMsg err: ", e.getLocalizedMessage());
        }
        return resultID;
    }

    private void sendToMainHandler(int i, String s){
        Log.d("sendToMainHandler: ", String.valueOf(i) + s);
        Message resultMsg = new Message();
        resultMsg.what = i;
        resultMsg.obj = s;
        mainHandler.sendMessage(resultMsg);
    }
}