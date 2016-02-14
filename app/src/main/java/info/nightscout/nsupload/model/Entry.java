package info.nightscout.nsupload.model;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by L on 2/14/2016.
 */
public class Entry {
    @SerializedName("date")
    public long timestamp;

    public double time_since_sensor_started;

    public double raw_data;

    public double filtered_data;

    public double age_adjusted_raw_value;

    public boolean calibration_flag;

    @SerializedName("custom_id")
    public double calculated_value;

    public double calculated_value_slope;

    public double a;
    public double b;

    public double c;

    public double ra;

    public double rb;

    public double rc;
    public String uuid;

    public String calibration_uuid;

    public String sensor_uuid;

    public boolean synced;

    public double raw_calculated;

    public boolean hide_slope;

    public String noise;

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        json.put("device", "X");
        json.put("date", timestamp);
        json.put("dateString", format.format(timestamp));
        json.put("sgv", Math.round(calculated_value));
        json.put("direction", "");
        json.put("type", "sgv");
        json.put("filtered", 0);
        json.put("unfiltered", 0);
        json.put("rssi", 100);
        json.put("noise", 0);

        json.put("sysTime", format.format(timestamp));
        return json;
    }
}
