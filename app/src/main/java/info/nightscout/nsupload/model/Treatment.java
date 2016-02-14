package info.nightscout.nsupload.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class Treatment {

    public double insulin;

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        json.put("created_at", format.format(created_at));
        json.put("eventType", eventType);
        json.put("duration", duration);
        json.put("percent", percent);
        json.put("insulin", insulin);
        json.put("enteredBy", enteredBy);

        return json;
    }

    public String enteredBy = "NSUploadLib-0.1/DanaApp";
    public String	eventType;
    public int	duration;
    public int	percent;
    public Date created_at;
}
