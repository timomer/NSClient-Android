package info.nightscout.nsupload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;

import info.nightscout.nsupload.model.Treatment;

public class TempBasalData extends BroadcastReceiver{
    private static Logger log = LoggerFactory.getLogger(TempBasalData.class);

    @Override
	public void onReceive(Context context, Intent intent) {
        log.debug("DANAAPP TempBasalData receive");
        Bundle bundle = intent.getExtras();

        TempBasal tempBasal = new TempBasal();
        tempBasal.timeStart = new Date(bundle.getLong("timeStart"));
        tempBasal.timeEnd = new Date(bundle.getLong("timeEnd"));
        tempBasal.baseRatio = bundle.getInt("baseRatio");
        tempBasal.tempRatio = bundle.getInt("tempRatio");
        tempBasal.percent = bundle.getInt("percent");

        log.debug(tempBasal.toString());

        try {
            final NightscoutUploader upload = new NightscoutUploader();
            final ArrayList<Treatment> records = new ArrayList<>();

            Treatment treatment = new Treatment();
            treatment.created_at = tempBasal.timeStart;
            treatment.percent = tempBasal.percent - 100;
            treatment.duration = (int) ((tempBasal.timeEnd.getTime() - tempBasal.timeStart.getTime())/60_000);
            treatment.eventType = "Temp Basal";
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
