package info.nightscout.client.services;


import android.content.Intent;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import info.nightscout.client.receivers.ReceiverBG;
import info.nightscout.nsupload.NightscoutUploader;
import info.nightscout.nsupload.model.Entry;


public class ServiceBG extends android.app.IntentService {
    private static Logger log = LoggerFactory.getLogger(ServiceBG.class);

    public static final DecimalFormat numberFormat = new DecimalFormat("0.00");
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

    public ServiceBG() {
        super("ServiceBG");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        try {
            Bundle bundle = intent.getExtras();
            Date time =  new Date(bundle.getLong("time"));
            int glucoseValue = bundle.getInt("value");
            int delta = bundle.getInt("delta");
            double deltaAvg30min = bundle.getDouble("deltaAvg30min");
            double deltaAvg15min = bundle.getDouble("deltaAvg15min");
            double avg30min = bundle.getDouble("avg30min");
            double avg15min = bundle.getDouble("avg15min");

            String msgReceived = "time:" + dateFormat.format(time)
                    + " bg " + glucoseValue
                    + " dlta: " + delta
                    + " dltaAvg30m:" + numberFormat.format(deltaAvg30min)
                    + " dltaAvg15m:" + numberFormat.format(deltaAvg15min)
                    + " avg30m:" + numberFormat.format(avg30min)
                    + " avg15m:" + numberFormat.format(avg15min);
            log.debug("onHandleIntent "+msgReceived);

            upload(time, glucoseValue);


        } catch (Throwable x){
            log.error(x.getMessage(),x);

        } finally {
            ReceiverBG.completeWakefulIntent(intent);
        }

    }

    public void upload(Date time, int glucoseValue) throws Exception {

        NightscoutUploader up = new NightscoutUploader();


        ArrayList<Entry> glucoseDataSets = new ArrayList<>();
        Entry bgReading = new Entry();
        bgReading.timestamp = time.getTime();
        bgReading.calculated_value = glucoseValue;

        glucoseDataSets.add(bgReading);
        up.doRESTUpload(glucoseDataSets);
    }
}
