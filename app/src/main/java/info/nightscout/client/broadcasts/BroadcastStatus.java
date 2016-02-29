package info.nightscout.client.broadcasts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.PowerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import info.nightscout.client.data.NSStatus;

/**
 * Created by mike on 24.02.2016.
 */
public class BroadcastStatus {
    private static Logger log = LoggerFactory.getLogger(BroadcastStatus.class);

    public void handleNewStatus(NSStatus status, Context context, boolean isDelta) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "sendQueue");
        wakeLock.acquire();
        try {
            Bundle bundle = new Bundle();
            bundle.putString("status", status.getData().toString());
            bundle.putBoolean("delta", isDelta);
            Intent intent = new Intent(Intents.ACTION_NEW_STATUS);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(intent);
            List<ResolveInfo> x = context.getPackageManager().queryBroadcastReceivers(intent, 0);

            log.debug("STATUS: " + x.size() + " receivers");

        } finally {
            wakeLock.release();
        }
    }


}
