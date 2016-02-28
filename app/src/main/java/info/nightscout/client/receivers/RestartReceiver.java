package info.nightscout.client.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import info.nightscout.client.MainApp;
import info.nightscout.client.data.UploadQueue;
import info.nightscout.client.events.RestartEvent;

public class RestartReceiver extends BroadcastReceiver {
    public RestartReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        MainApp.bus().post(new RestartEvent());
    }
}
