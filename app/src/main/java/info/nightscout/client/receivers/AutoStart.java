package info.nightscout.client.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import info.nightscout.client.services.ServiceNS;

public class AutoStart extends BroadcastReceiver {
    public AutoStart() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, ServiceNS.class));
    }
}
