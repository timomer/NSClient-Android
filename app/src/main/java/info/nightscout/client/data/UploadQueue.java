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

/**
 * Created by mike on 21.02.2016.
 */
public class UploadQueue {
    private static Logger log = LoggerFactory.getLogger(UploadQueue.class);

    public static HashMap<String,DbUpdateRequest> updateQueue = null;
    public static HashMap<String,DbUpdateRequest> updateUnsetQueue = null;
    public static HashMap<String,DbAddRequest> addQueue = null;
    public static HashMap<String,DbRemoveRequest> removeQueue = null;

    public UploadQueue() {
        if (updateQueue == null) updateQueue = new HashMap<String,DbUpdateRequest>();
        if (updateUnsetQueue == null) updateUnsetQueue = new HashMap<String,DbUpdateRequest>();
        if (addQueue == null) addQueue = new HashMap<String,DbAddRequest>();
        if (removeQueue == null) removeQueue = new HashMap<String,DbRemoveRequest>();
    }

    public static String status() {
        return "QUEUE Add: " + addQueue.size() + " Remove: " + removeQueue.size() + " Update: " + updateQueue.size() + " Unset: " + updateUnsetQueue.size() ;
    }

    public static void reset() {
        log.debug("QUEUE Reset");
        updateQueue.clear();
        updateUnsetQueue.clear();
        addQueue.clear();
        removeQueue.clear();
        log.debug(status());
    }

    public static void resend() {
        if (addQueue.size() == 0 && removeQueue.size() == 0 &&  updateQueue.size() ==0 && updateUnsetQueue.size() == 0)
            return;

        NSClient nsClient = MainApp.getNSClient();
        log.debug("QUEUE Resend started");

        Iterator<Map.Entry<String,DbAddRequest>> addIter = addQueue.entrySet().iterator();
        while (addIter.hasNext()) {
            DbAddRequest dbar = addIter.next().getValue();
            NSAddAck addAck = new NSAddAck();
            nsClient.dbAdd(dbar, addAck);
            if (addAck._id == null) {
                log.debug("QUEUE No response on dbAdd");
                log.debug(UploadQueue.status());
                return;
            }
            String key = dbar.data.optString("created_at") + " " + dbar.data.optString("eventType");
            log.debug("QUEUE dbAdd processed: " + key);
            addIter.remove();
        }

        Iterator<Map.Entry<String,DbRemoveRequest>> removeIter = removeQueue.entrySet().iterator();
        while (removeIter.hasNext()) {
            DbRemoveRequest dbrr = removeIter.next().getValue();
            NSUpdateAck removeAck = new NSUpdateAck();
            nsClient.dbRemove(dbrr, removeAck);
            if (!removeAck.result) {
                log.debug("QUEUE No response on dbRemove");
                log.debug(UploadQueue.status());
                return;
            }
            log.debug("QUEUE dbRemove processed: " + dbrr._id);
            removeIter.remove();
        }

        Iterator<Map.Entry<String,DbUpdateRequest>> updateIter = updateQueue.entrySet().iterator();
        while (updateIter.hasNext()) {
            DbUpdateRequest dbur = updateIter.next().getValue();
            NSUpdateAck updateAck = new NSUpdateAck();
            nsClient.dbUpdate(dbur, updateAck);
            if (!updateAck.result) {
                log.debug("QUEUE No response on dbUpdate");
                log.debug(UploadQueue.status());
                return;
            }
            log.debug("QUEUE dbUpdate processed: " + dbur._id);
            updateIter.remove();
        }

        Iterator<Map.Entry<String,DbUpdateRequest>> updateUnsetIter = updateUnsetQueue.entrySet().iterator();
        while (updateUnsetIter.hasNext()) {
            DbUpdateRequest dbur = updateIter.next().getValue();
            NSUpdateAck updateUnsetAck = new NSUpdateAck();
            nsClient.dbUpdateUnset(dbur, updateUnsetAck);
            if (!updateUnsetAck.result) {
                log.debug("QUEUE No response on dbUpdateUnset");
                log.debug(UploadQueue.status());
                return;
            }
            log.debug("QUEUE dbUpdateUnset processed: " + dbur._id);
            updateUnsetIter.remove();
        }

        log.debug(UploadQueue.status());
    }

}
