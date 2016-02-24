package info.nightscout.client.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.client.MainApp;
import info.nightscout.client.NSClient;
import info.nightscout.client.acks.NSAddAck;
import info.nightscout.client.acks.NSUpdateAck;
import info.nightscout.client.data.DbAddRequest;
import info.nightscout.client.data.DbRemoveRequest;
import info.nightscout.client.data.DbUpdateRequest;
import info.nightscout.client.data.UploadQueue;

public class DBAccessReceiver extends BroadcastReceiver {
    private static Logger log = LoggerFactory.getLogger(DBAccessReceiver.class);


    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundles = intent.getExtras();
        if (bundles == null) return;
        if (!bundles.containsKey("action")) return;

        NSClient nsClient = MainApp.getNSClient();
        if (nsClient == null) return;

        String collection = null;
        String _id = null;
        JSONObject data = null;
        String action = bundles.getString("action");
        try { collection = bundles.getString("collection"); } catch (Exception e) {}
        try { _id = bundles.getString("_id"); } catch (Exception e) {}
        try { data = new JSONObject(bundles.getString("data")); } catch (Exception e) {}

        if (action.equals("dbAdd")) {
            if (!isAllowedCollection(collection)) {
                log.debug("DBACCESS wrong collection specified");
                return;
            }
            if (!data.has("created_at") && collection.equals("treatments")) {
                log.debug("DBACCESS created_at missing on dbAdd");
                return;
            }
            if (!data.has("eventType") && collection.equals("treatments")) {
                log.debug("DBACCESS eventType missing on dbAdd");
                return;
            }
            DbAddRequest dbar = new DbAddRequest();
            dbar.collection = collection;
            dbar.data = data;
            NSAddAck addAck = new NSAddAck();
            nsClient.dbAdd(dbar, addAck);
            String key = dbar.data.optString("created_at") + " " + dbar.data.optString("eventType");
            if (addAck._id == null) {
                log.debug("DBACCESS No response on dbAdd");
                UploadQueue.addQueue.put(key, dbar);
                log.debug(UploadQueue.status());
                return;
            }
            log.debug("DBACCESS dbAdd processed: " + key);
        }

        if (action.equals("dbRemove")) {
            if (!isAllowedCollection(collection)) {
                log.debug("DBACCESS wrong collection specified");
                return;
            }
            DbRemoveRequest dbrr = new DbRemoveRequest();
            dbrr.collection = collection;
            dbrr._id = _id;
            NSUpdateAck updateAck = new NSUpdateAck();
            nsClient.dbRemove(dbrr, updateAck);
            if (!updateAck.result) {
                log.debug("DBACCESS No response on dbRemove");
                UploadQueue.removeQueue.put(_id, dbrr);
                log.debug(UploadQueue.status());
                return;
            }
            log.debug("DBACCESS dbRemove processed: " + _id);
        }

        if (action.equals("dbUpdate")) {
            if (!isAllowedCollection(collection)) {
                log.debug("DBACCESS wrong collection specified");
                return;
            }
            DbUpdateRequest dbur = new DbUpdateRequest();
            dbur.collection = collection;
            dbur._id = _id;
            dbur.data = data;
            NSUpdateAck updateAck = new NSUpdateAck();
            nsClient.dbUpdate(dbur, updateAck);
            if (!updateAck.result) {
                log.debug("DBACCESS No response on dbUpdate");
                UploadQueue.updateQueue.put(_id, dbur);
                log.debug(UploadQueue.status());
                return;
            }
            log.debug("DBACCESS dbUpdate processed: " + _id);
        }

        if (action.equals("dbUpdateUnset")) {
            if (!isAllowedCollection(collection)) {
                log.debug("DBACCESS wrong collection specified");
                return;
            }
            DbUpdateRequest dbur = new DbUpdateRequest();
            dbur.collection = collection;
            dbur._id = _id;
            dbur.data = data;
            NSUpdateAck updateAck = new NSUpdateAck();
            nsClient.dbUpdateUnset(dbur, updateAck);
            if (!updateAck.result) {
                log.debug("DBACCESS No response on dbUpdateUnset");
                UploadQueue.updateUnsetQueue.put(_id, dbur);
                log.debug(UploadQueue.status());
                return;
            }
            log.debug("DBACCESS dbUpdateUnset processed: " + _id);
        }

    }

    private boolean isAllowedCollection(String collection) {
        // "treatments" || "entries" || "devicestatus" || "profile" || "food"
        if (collection.equals("treatments")) return true;
        if (collection.equals("entries")) return true;
        if (collection.equals("devicestatus")) return true;
        if (collection.equals("profile")) return true;
        if (collection.equals("food")) return true;
        return false;
    }
}
