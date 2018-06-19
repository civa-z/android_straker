package com.civasony.stracker;


import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ViewDebug;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
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
    }

    public void setIp(String ip) {
        this.ip = ip;
        strUrlPath = "http://" + ip + ":3010/recognize?timeout=50";
        try {
            url = new URL(strUrlPath);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "audio/x-wav; rate=16000");
            conn.setDoOutput(true); //如果要输出，则必须加上此句

            out = conn.getOutputStream();
            in = conn.getInputStream();
        }catch (Exception e){
            Log.d("setIp err: ", e.getLocalizedMessage());
            sendToMainHandler(-1, e.getLocalizedMessage());
        }
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
                sendToServer(1,ObjectAndByte.toByteArray(msg.obj));
                int information = receiveFromServer();
                sendToMainHandler(-1, Integer.toString(-1));
            }
        };
        Looper.loop();
    }

    private boolean sendToServer(int fileId, byte[] imageBuf){
        try {
            Log.d("PostThread: ", "sendPostMsg");
            DataInputStream dos = new DataInputStream(new ByteArrayInputStream(imageBuf));
            byte[] tempBuffer = new byte[bufferSize];
            while(true){
                int len = dos.read(tempBuffer);
                if (len > 0)
                    out.write(tempBuffer);
                else
                    break;
            }
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
                Log.d("PostThread: ", "200");
                InputStream inputStream = new BufferedInputStream(in);
                String result = "";
                while (true) {
                    byte[] inBuff = new byte[bufferSize];
                    int len = inputStream.read(inBuff);
                    if (len <= 0)
                        break;
                    result = result + new String(inBuff, "utf-8");
                }
				JSONObject jsonObject = new JSONObject(result);
                resultID = jsonObject.getInt("id");
            }
        }catch (Exception e){
            Log.d("getResponseMsg err: ", e.getLocalizedMessage());
        }
        return resultID;
    }

    private void sendToMainHandler(int i, String s){
        Message resultMsg = new Message();
        resultMsg.what = i;
        resultMsg.obj = s;
        mainHandler.sendMessage(resultMsg);
    }
}