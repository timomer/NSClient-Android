package info.nightscout.nsupload;

import android.util.Log;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import info.nightscout.nsupload.model.Entry;
import info.nightscout.nsupload.model.Treatment;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public class NightscoutUploader {
    private OkHttpClient client;
    private final NightscoutService nightscoutService;
    private final String hashedSecret;


    public interface NightscoutService {
        @POST("entries")
        Call<ResponseBody> uploadEntries(@Header("api-secret") String secret, @Body RequestBody body);

        @POST("treatments")
        Call<ResponseBody> uploadTreatments(@Header("api-secret") String secret, @Body RequestBody body);

        @POST("devicestatus")
        Call<ResponseBody> uploadDeviceStatus(@Body RequestBody body);

        @POST("devicestatus")
        Call<ResponseBody> uploadDeviceStatus(@Header("api-secret") String secret, @Body RequestBody body);

    }

    private class UploaderException extends RuntimeException {
        int code;

        public UploaderException(String message, int code) {
            super(message);
            this.code = code;
        }
    }

    public NightscoutUploader(String baseURI) throws Exception {
        client = (new OkHttpClient.Builder()).connectionPool(new ConnectionPool(1, 5, TimeUnit.MILLISECONDS)).build();


        URI uri = new URI(baseURI);

        String baseURL;
        String secret = uri.getUserInfo();

        baseURL = baseURI.replaceFirst("//[^@]+@", "//");

        Retrofit retrofit = new Retrofit.Builder().baseUrl(baseURL).client(client).build();
        nightscoutService = retrofit.create(NightscoutService.class);

        hashedSecret = Hashing.sha1().hashBytes(secret.getBytes(Charsets.UTF_8)).toString();
    }

    public boolean doRESTUpload(List<Entry> glucoseDataSets) throws Exception {

        doRESTUploadTo(glucoseDataSets);

        return true;
    }



    private void doRESTUploadTo(List<Entry> glucoseDataSets) throws Exception {
        uploadEntries(glucoseDataSets);

        uploadDeviceStatus();
    }

    public void uploadEntries( List<Entry> entries) throws JSONException, java.io.IOException {
        JSONArray array = new JSONArray();
        for (Entry record : entries) {
            array.put(record.toJSON());
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), array.toString());
        Response<ResponseBody> r = nightscoutService.uploadEntries(hashedSecret, body).execute();
        if (!r.isSuccess()) {
            throw new UploaderException(r.message(), r.code());
        }
    }

    public void uploadTreatments(List<Treatment> records) throws JSONException, java.io.IOException {
        JSONArray array = new JSONArray();
        for (Treatment record : records) {
            array.put(record.toJSON());
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), array.toString());
        Response<ResponseBody> r = nightscoutService.uploadTreatments(hashedSecret, body).execute();
        if (!r.isSuccess()) {
            throw new UploaderException(r.message(), r.code());
        }
    }


    private void uploadDeviceStatus() throws Exception {
        JSONObject json = new JSONObject();
        json.put("uploaderBattery", 100);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
        Response<ResponseBody> r;
        r = nightscoutService.uploadDeviceStatus(hashedSecret, body).execute();
        if (!r.isSuccess()) {
            throw new UploaderException(r.message(), r.code());
        }
    }



}
