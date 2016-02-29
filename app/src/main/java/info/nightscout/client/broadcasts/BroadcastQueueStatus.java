package info.nightscout.client.broadcasts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.PowerManager;

import java.util.List;

import info.nightscout.client.data.NSProfile;

/**
 * Created by mike on 28.02.2016.
 */
public class BroadcastQueueStatus {
    public void handleNewStatus(int size, Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "sendQueue");
        wakeLock.acquire();
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("size", size);
            Intent intent = new Intent(Intents.ACTION_QUEUE_STATUS);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(intent);
        } finally {
            wakeLock.release();
        }
    }
}
