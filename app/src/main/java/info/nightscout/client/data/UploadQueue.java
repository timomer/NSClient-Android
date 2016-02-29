package info.nightscout.client.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import info.nightscout.client.MainApp;
import info.nightscout.client.NSClient;
import info.nightscout.client.acks.NSAddAck;
import info.nightscout.client.acks.NSUpdateAck;
import info.nightscout.client.broadcasts.BroadcastQueueStatus;
import info.nightscout.client.broadcasts.BroadcastStatus;

/**
 * Created by mike on 21.02.2016.
 */
public class UploadQueue {
    private static Logger log = LoggerFactory.getLogger(UploadQueue.class);

    public static HashMap<String,DbRequest> queue = null;

    private static boolean resendRunning = false;

    public UploadQueue() {
        if (queue == null) queue = new HashMap<String,DbRequest>();
    }

    public static String status() {
        return "QUEUE: " + queue.size();
    }

    public static void put(String hash, DbRequest dbr) {
        queue.put(hash, dbr);
        BroadcastQueueStatus bs = new BroadcastQueueStatus();
        bs.handleNewStatus(queue.size(), MainApp.instance().getApplicationContext());
    }

    public static void reset() {
        log.debug("QUEUE Reset");
        queue.clear();
        log.debug(status());
    }

    public static void resend() {
        if (queue.size() == 0)
            return;

        if (resendRunning) return;
        resendRunning = true;

        final NSClient nsClient = MainApp.getNSClient();
        log.debug("QUEUE Resend started");

        Thread doResend = new Thread() {
            public void run() {
                Iterator<Map.Entry<String,DbRequest>> iter = queue.entrySet().iterator();
                while (iter.hasNext()) {
                    DbRequest dbr = iter.next().getValue();
                    if (dbr.action.equals("dbAdd")) {
                        NSAddAck addAck = new NSAddAck();
                        nsClient.dbAdd(dbr, addAck);
                        if (addAck._id == null) {
                            log.debug("QUEUE No response on dbAdd");
                            log.debug(UploadQueue.status());
                            return;
                        }
                        log.debug("QUEUE dbAdd processed: " + dbr.data.toString());
                        iter.remove();
                        log.debug(UploadQueue.status());
                    } else if (dbr.action.equals("dbRemove")) {
                        NSUpdateAck removeAck = new NSUpdateAck();
                        nsClient.dbRemove(dbr, removeAck);
                        if (!removeAck.result) {
                            log.debug("QUEUE No response on dbRemove");
                            log.debug(UploadQueue.status());
                            return;
                        }
                        log.debug("QUEUE dbRemove processed: " + dbr._id);
                        iter.remove();
                        log.debug(UploadQueue.status());
                    } else if (dbr.action.equals("dbUpdate")) {
                        NSUpdateAck updateAck = new NSUpdateAck();
                        nsClient.dbUpdate(dbr, updateAck);
                        if (!updateAck.result) {
                            log.debug("QUEUE No response on dbUpdate");
                            log.debug(UploadQueue.status());
                            return;
                        }
                        log.debug("QUEUE dbUpdate processed: " + dbr._id);
                        iter.remove();
                        log.debug(UploadQueue.status());
                    } else if (dbr.action.equals("dbUpdateUnset")) {
                        NSUpdateAck updateUnsetAck = new NSUpdateAck();
                        nsClient.dbUpdateUnset(dbr, updateUnsetAck);
                        if (!updateUnsetAck.result) {
                            log.debug("QUEUE No response on dbUpdateUnset");
                            log.debug(UploadQueue.status());
                            return;
                        }
                        log.debug("QUEUE dbUpdateUnset processed: " + dbr._id);
                        iter.remove();
                        log.debug(UploadQueue.status());
                    }
                }
            }
        };
        doResend.start();
        try {
            doResend.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        resendRunning = false;
    }

}
