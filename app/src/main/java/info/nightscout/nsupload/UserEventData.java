package info.nightscout.nsupload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import info.nightscout.nsupload.model.Treatment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;

public class UserEventData extends BroadcastReceiver{
    private static Logger log = LoggerFactory.getLogger(UserEventData.class);

    @Override
	public void onReceive(Context context, Intent intent) {
        log.debug("DANAAPP TreatmentReceive");
        Bundle bundle = intent.getExtras();
        long time = bundle.getLong("time");
        int value = bundle.getInt("value");


        try {
            final NightscoutUploader upload = new NightscoutUploader();
            final ArrayList<Treatment> records = new ArrayList<>();

            Treatment treatment = new Treatment();
            treatment.created_at = new Date(time);
            treatment.insulin = value/100d;

            treatment.eventType = "Correction Bolus";
            records.add(treatment);
            (new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        upload.uploadTreatments(records);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

//                upload.uploadTreatments(records);
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
