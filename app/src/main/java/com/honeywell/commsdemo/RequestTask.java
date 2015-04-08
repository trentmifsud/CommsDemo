package com.honeywell.commsdemo;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by E841242 on 28/01/2015.
 */
public class RequestTask extends AsyncTask<String, String, String> {

    public AsyncResponse delegate=null;

    @Override
    protected void onPostExecute(String result)
    {
       // super.onPostExecute(result);
        delegate.processFinish(result);
    }

    @Override
    protected String doInBackground(String... uri)
    {
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;

        try
        {
            response = httpClient.execute(new HttpGet(uri[0]));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK)
            {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                responseString = out.toString();
                out.close();
            }
            else
            {
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }

        }
        catch(ClientProtocolException e)
        {
            //throw e;
        }
        catch(IOException e)
        {

        }
        return responseString;
    }
}
