package info.nightscout.client.broadcasts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;


import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import info.nightscout.client.MainApp;
import info.nightscout.client.data.NSTreatment;

/**
 * Created by mike on 20.02.2016.
 */
public class BroadcastTreatment {
    private static Logger log = LoggerFactory.getLogger(BroadcastTreatment.class);

    public void handleNewTreatment(NSTreatment treatment, Context context, boolean isDelta) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "sendQueue");
        wakeLock.acquire();
        try {
            Bundle bundle = new Bundle();
            bundle.putString("treatment", treatment.getData().toString());
            bundle.putBoolean("delta", isDelta);
            Intent intent = new Intent(Intents.ACTION_NEW_TREATMENT);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(intent);
            List<ResolveInfo> x = context.getPackageManager().queryBroadcastReceivers(intent, 0);

            log.debug("TREAT " + treatment.getEventType() + " " + x.size() + " receivers");

            } finally {
            wakeLock.release();
        }
    }

    public void handleChangedTreatment(JSONObject treatment, Context context, boolean isDelta) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "sendQueue");
        wakeLock.acquire();
        try {
            Bundle bundle = new Bundle();
            bundle.putString("treatment", treatment.toString());
            bundle.putBoolean("delta", isDelta);
            Intent intent = new Intent(Intents.ACTION_CHANGED_TREATMENT);
            intent.putExtras(bundle);
            context.sendBroadcast(intent);
            List<ResolveInfo> x = context.getPackageManager().queryBroadcastReceivers(intent, 0);

            try {
                log.debug("TREAT_CHANGE " + treatment.getString("_id") + " " + x.size() + " receivers");
            } catch (JSONException e) {}

            } finally {
            wakeLock.release();
        }
    }

    public void handleRemovedTreatment(JSONObject treatment, Context context, boolean isDelta) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "sendQueue");
        wakeLock.acquire();
        try {
            Bundle bundle = new Bundle();
            bundle.putString("treatment", treatment.toString());
            bundle.putBoolean("delta", isDelta);
            Intent intent = new Intent(Intents.ACTION_REMOVED_TREATMENT);
            intent.putExtras(bundle);
            context.sendBroadcast(intent);
            List<ResolveInfo> x = context.getPackageManager().queryBroadcastReceivers(intent, 0);

            try {
                log.debug("TREAT_REMOVE " + treatment.getString("_id") + " " + x.size() + " receivers");
            } catch (JSONException e) {}

        } finally {
            wakeLock.release();
        }
    }

}
