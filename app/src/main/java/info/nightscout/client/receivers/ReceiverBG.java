package info.nightscout.client.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.client.services.ServiceBG;

public class ReceiverBG extends WakefulBroadcastReceiver {
    private static Logger log = LoggerFactory.getLogger(ReceiverBG.class);

    @Override
	public void onReceive(Context context, Intent intent) {
        log.debug("onReceive "+intent);
        startWakefulService(context, new Intent(context, ServiceBG.class)
                .setAction(intent.getAction())
                .putExtras(intent));
    }

}
