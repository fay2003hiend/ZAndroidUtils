package com.dx.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;

import android.os.Environment;

public class ZHttpClient {
    final String TAG = "ZHttpClient";
    String ServerURL;

    List<NameValuePair> mParams = new ArrayList<NameValuePair>();

    public ZHttpClient(String url) {
        ServerURL = url;
    }

    public void addParam(String key, String value) {
        mParams.add(new BasicNameValuePair(key, value));
    }

    public void addParam(Map<String, String> map) {
        Set<Entry<String, String>> sets = map.entrySet();
        for (Entry<String, String> entry : sets) {
            mParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
    }

    public void clearParams() {
        mParams.clear();
    }

    public String request() {
        return request(mParams);
    }

    public boolean request(DataReciever reciever, InputStreamReciver is_reciever) {
        return request(mParams, reciever, is_reciever);
    }

    public interface DataReciever {
        public void onSizeRecieved(long size);

        public void onDataRecieved(DefaultHttpClient client, byte[] data, int count);
    }

    public interface InputStreamReciver {
        public void onSizeRecieved(long size);

        public void handleStream(DefaultHttpClient client, InputStream is);
    }

    class MDataReciever implements DataReciever {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        @Override
        public void onSizeRecieved(long size) {

        }

        @Override
        public void onDataRecieved(DefaultHttpClient client, byte[] data, int count) {
            bos.write(data, 0, count);
        }
    }

    public String request(List<NameValuePair> params) {
        MDataReciever r = new MDataReciever();
        if (request(params, r, null)) {
            String ret = r.bos.toString();
            Log.w(TAG, "===>" + ret);
            return ret;
        } else {
            return null;
        }
    }

    public byte[] requestToByteArray() {
        MDataReciever r = new MDataReciever();
        if (request(mParams, r, null)) {
            byte[] ret = r.bos.toByteArray();
            Log.hex(TAG, ret, 0, ret.length);
            return ret;
        } else {
            return null;
        }
    }

    public boolean request(List<NameValuePair> params, DataReciever data_reciever, InputStreamReciver ins_reciever) {
        for (NameValuePair p : params) {
            Log.w(TAG, p.getName() + "=" + p.getValue());
        }

        try {
            UrlEncodedFormEntity url_entity = new UrlEncodedFormEntity(params, "UTF-8");
            return requestByEntity(url_entity, data_reciever, ins_reciever);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean requestByEntity(HttpEntity reqEntity, DataReciever data_reciever, InputStreamReciver ins_reciever) {
        try {

            HttpPost request = new HttpPost(ServerURL);
            request.setEntity(reqEntity);
            request.setHeader("Accept-Encoding", "gzip");

            DefaultHttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
            client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);

            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                Log.w(TAG, "response code invalid: " + response.getStatusLine().getStatusCode());
                return false;
            }
            HttpEntity entity = response.getEntity();
            long content_length = entity.getContentLength();
            Log.w(TAG, "content_length: " + content_length);
            if (data_reciever != null) {
                data_reciever.onSizeRecieved(content_length);
            }
            if (ins_reciever != null) {
                ins_reciever.onSizeRecieved(content_length);
            }

            Header contentEncoding = entity.getContentEncoding();
            InputStream is = entity.getContent();
            if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                Log.w(TAG, "using GZIP.");
                is = new GZIPInputStream(is);
            }

            if (data_reciever != null) {
                byte[] buffer = new byte[4096];
                while (true) {
                    int count = is.read(buffer);
                    if (count <= 0) {
                        Log.w(TAG, "read: " + count);
                        break;
                    }

                    Log.i(TAG, "read: " + count);
                    data_reciever.onDataRecieved(client, buffer, count);
                }
            } else if (ins_reciever != null) {
                ins_reciever.handleStream(client, is);
            } else {
                client.getConnectionManager().shutdown();
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void saveLog(String ret, String name) throws Exception {
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String logPath = Environment.getExternalStorageDirectory() + "/" + name;
        android.util.Log.i("", "upload path = " + logPath);

        FileOutputStream fw = new FileOutputStream(logPath, true);
        fw.write(("\n--TIME-- " + now + " ----\n").getBytes());
        fw.write(ServerURL.getBytes());
        fw.write("\n----\n".getBytes());

        UrlEncodedFormEntity url_entity = new UrlEncodedFormEntity(mParams, "UTF-8");
        InputStream is = url_entity.getContent();

        byte buf[] = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) {
            fw.write(buf, 0, n);
        }

        fw.write(("\n--RESP-- " + ret + " ----\n").getBytes());

        fw.flush();
        fw.close();
        is.close();
    }
}
