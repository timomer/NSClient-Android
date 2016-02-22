package info.nightscout.client.acks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;

/**
 * Created by mike on 21.02.2016.
 */
public class NSUpdateAck implements Ack {
    public boolean result = false;
    public void call(Object...args) {
        JSONObject response = (JSONObject)args[0];
        if (response.has("result"))
            try {
                if (response.getString("result").equals("success"))
                    result = true;
            } catch (JSONException e) {
            }
        synchronized(this) {
            this.notify();
        }
    }
}
