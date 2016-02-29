package info.nightscout.client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.UtilityModels.XDripEmulator;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.squareup.otto.Bus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

import info.nightscout.client.acks.NSAddAck;
import info.nightscout.client.acks.NSAuthAck;
import info.nightscout.client.acks.NSUpdateAck;
import info.nightscout.client.broadcasts.BroadcastProfile;
import info.nightscout.client.broadcasts.BroadcastSgvs;
import info.nightscout.client.broadcasts.BroadcastStatus;
import info.nightscout.client.broadcasts.BroadcastTreatment;
import info.nightscout.client.data.DbRequest;
import info.nightscout.client.data.NSCal;
import info.nightscout.client.data.NSSgv;
import info.nightscout.client.data.NSStatus;
import info.nightscout.client.data.NSTreatment;
import info.nightscout.client.data.UploadQueue;
import info.nightscout.client.events.NSStatusEvent;
import info.nightscout.client.data.NSProfile;
import info.nightscout.client.tests.TestReceiveID;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class NSClient {
    private static Integer dataCounter = 0;

    private Bus mBus;
    private static Logger log = LoggerFactory.getLogger(NSClient.class);
    private Socket mSocket;
    private boolean isConnected = false;
    private String connectionStatus = "Not connected";

    private boolean nsEnabled = false;
    private String nsURL = "";
    private String nsAPISecret = "";
    private String nsDevice = "";
    private Integer nsHours = 1;

    private final Integer timeToWaitForResponseInMs = 10000;
    private boolean uploading = false;

    private String nsAPIhashCode = "";

    public NSClient(Bus bus) {
        MainApp.setNSClient(this);
        mBus = bus;

        dataCounter = 0;

        readPreferences();

        if (nsAPISecret!="") nsAPIhashCode = Hashing.sha1().hashString(nsAPISecret, Charsets.UTF_8).toString();

        Intent i = new Intent(MainApp.instance().getApplicationContext(), PreferencesActivity.class);
        mBus.post(new NSStatusEvent(connectionStatus));
        if (!nsEnabled) {
            log.debug("NSCLIENT disabled");
            connectionStatus = "Disabled";
            mBus.post(new NSStatusEvent(connectionStatus));
            Toast.makeText(MainApp.instance().getApplicationContext(), "NS connection disabled", Toast.LENGTH_LONG).show();
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            MainApp.instance().getApplicationContext().startActivity(i);
        } else if (nsURL != "") {
            try {
                connectionStatus = "Connecting ...";
                mBus.post(new NSStatusEvent(connectionStatus));
                IO.Options opt = new IO.Options();
                opt.forceNew = true;
                mSocket = IO.socket(nsURL, opt);
                log.debug("NSCLIENT connect");
                mSocket.connect();
                mSocket.on("dataUpdate", onDataUpdate);
                mSocket.on("ping", onPing);
                // resend auth on reconnect is needed on server restart
                //mSocket.on("reconnect", resendAuth);
                sendAuthMessage(new NSAuthAck());
                log.debug("NSCLIENT start");
            } catch (URISyntaxException e) {
                log.debug("NSCLIENT Wrong URL syntax");
                connectionStatus = "Wrong URL syntax";
                mBus.post(new NSStatusEvent(connectionStatus));
                Toast.makeText(MainApp.instance().getApplicationContext(), "Wrong URL syntax", Toast.LENGTH_LONG).show();
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MainApp.instance().getApplicationContext().startActivity(i);
            }
        } else {
            log.debug("NSCLIENT No NS URL specified");
            connectionStatus = "Disabled";
            mBus.post(new NSStatusEvent(connectionStatus));
            Toast.makeText(MainApp.instance().getApplicationContext(), "No NS URL specified", Toast.LENGTH_LONG).show();
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            MainApp.instance().getApplicationContext().startActivity(i);
        }
    }

    public void destroy() {
        MainApp.bus().unregister(this);
        if (mSocket != null) {
            log.debug("NSCLIENT destroy");
            mSocket.disconnect();
            mSocket = null;
            MainApp.setNSClient(null);
        }
    }

    public void sendAuthMessage(NSAuthAck ack) {
        JSONObject authMessage = new JSONObject();
        try {
            authMessage.put("client", "Android_" + nsDevice);
            authMessage.put("history", nsHours);
            authMessage.put("status", true); // receive status
            authMessage.put("pingme", true); // send mi pings to keep alive
            authMessage.put("secret", nsAPIhashCode);
        } catch (JSONException e) {
            return;
        }
        log.debug("NSCLIENT authorize " + mSocket.id());
        try {
            mSocket.emit("authorize", authMessage, ack);
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized(ack) {
            try {
                ack.wait(timeToWaitForResponseInMs);
            } catch (InterruptedException e) {
                ack.interrupted = true;
            }
        }
        if (ack.interrupted) {
            log.debug("NSCLIENT Auth interrupted");
            isConnected = false;
            connectionStatus = "Auth interrupted";
            mBus.post(new NSStatusEvent(connectionStatus));
        }
        else if (ack.received){
            log.debug("NSCLIENT Authenticated");
            connectionStatus = "Authenticated (";
            if (ack.read) connectionStatus += "R";
            if (ack.write) connectionStatus += "W";
            if (ack.write_treatment) connectionStatus += "T";
            connectionStatus += ')';
            isConnected = true;
            mBus.post(new NSStatusEvent(connectionStatus));
        } else {
            log.debug("NSCLIENT Auth timed out "  + mSocket.id());
            isConnected = true;
            connectionStatus = "Auth timed out";
            mBus.post(new NSStatusEvent(connectionStatus));
            return;
        }
    }

    public void readPreferences() {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
        try { nsEnabled = SP.getBoolean("ns_enable", false); } catch(Exception e) {}
        try { nsURL = SP.getString("ns_url", ""); } catch(Exception e) {}
        try { nsAPISecret = SP.getString("ns_api_secret", ""); } catch(Exception e) {}
        try { nsHours = SP.getInt("ns_api_hours", 1); } catch(Exception e) {}
    }

    private Emitter.Listener onPing = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (Config.detailedLog) log.debug("NSCLIENT Ping received");
            // send data if there is something waiting
            UploadQueue.resend();
        }
    };

    private Emitter.Listener onDataUpdate = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
            boolean emulatexDrip = SP.getBoolean("ns_emulate_xdrip", false);
            connectionStatus = "Data packet " + dataCounter++;
            mBus.post(new NSStatusEvent(connectionStatus));

            JSONObject data = (JSONObject) args[0];
            String activeProfile = MainApp.getNsActiveProfile();
            NSCal actualCal = new NSCal();
            try {
                // delta means only increment/changes are comming
                boolean isDelta =  data.has("delta");
                boolean isFull = !isDelta;
                log.debug("NSCLIENT Data packet #" + dataCounter + (isDelta ? " delta" : " full"));

                if (data.has("status")) {
                    JSONObject status = data.getJSONObject("status");
                    NSStatus nsStatus = new NSStatus(status);
                    activeProfile = nsStatus.getActiveProfile();
                    MainApp.setNsActiveProfile(activeProfile);
                    if (activeProfile != null) {
                        log.debug("NSCLIENT status activeProfile received: " + activeProfile);
                    }
                    BroadcastStatus bs = new BroadcastStatus();
                    bs.handleNewStatus(nsStatus, MainApp.instance().getApplicationContext(), isDelta);
                    /*  Other received data to 2016/02/10
                        {
                          status: 'ok'
                          , name: env.name
                          , version: env.version
                          , versionNum: versionNum (for ver 1.2.3 contains 10203)
                          , serverTime: new Date().toISOString()
                          , apiEnabled: apiEnabled
                          , careportalEnabled: apiEnabled && env.settings.enable.indexOf('careportal') > -1
                          , boluscalcEnabled: apiEnabled && env.settings.enable.indexOf('boluscalc') > -1
                          , head: env.head
                          , settings: env.settings
                          , extendedSettings: ctx.plugins && ctx.plugins.extendedClientSettings ? ctx.plugins.extendedClientSettings(env.extendedSettings) : {}
                          , activeProfile ..... calculated from treatments or missing
                        }
                     */
                }
                if (data.has("profiles")) {
                    JSONArray profiles = (JSONArray) data.getJSONArray("profiles");
                    BroadcastProfile bp = new BroadcastProfile();
                    if (profiles.length() > 0) {
                        JSONObject profile = (JSONObject) profiles.get(profiles.length() - 1);
                        NSProfile nsProfile = new NSProfile(profile,activeProfile);
                        MainApp.setNsProfile(nsProfile);
                        log.debug("NSCLIENT profile received: " + nsProfile.log());
                        bp.handleNewTreatment(nsProfile, MainApp.instance().getApplicationContext(), isDelta);
                    }
                }
                if (data.has("treatments")) {
                    JSONArray treatments = (JSONArray) data.getJSONArray("treatments");
                    BroadcastTreatment bt = new BroadcastTreatment();
                    if (treatments.length() > 0) log.debug("NSCLIENT received " + treatments.length() + " treatments");
                    for (Integer index = 0; index < treatments.length(); index++) {
                        JSONObject jsonTreatment = treatments.getJSONObject(index);
                        NSTreatment treatment =  new NSTreatment(jsonTreatment);
                        if (treatment.getAction() == null) {
                            // ********** TEST CODE ***********
                            if (jsonTreatment.has("NSCLIENTTESTRECORD")) {
                                MainApp.bus().post(new TestReceiveID(jsonTreatment.getString("_id")));
                                log.debug("----- Broadcasting test _id -----");
                                continue;
                            }
                            // ********* TEST CODE END ********
                            bt.handleNewTreatment(treatment,MainApp.instance().getApplicationContext(), isDelta);
                        } else if (treatment.getAction().equals("update")) {
                            bt.handleChangedTreatment(jsonTreatment, MainApp.instance().getApplicationContext(), isDelta);
                        } else if (treatment.getAction().equals("remove")) {
                            bt.handleRemovedTreatment(jsonTreatment, MainApp.instance().getApplicationContext(), isDelta);
                        }
                    }
                }
                if (data.has("devicestatus")) {
                    JSONArray devicestatuses = (JSONArray) data.getJSONArray("devicestatus");
                    if (devicestatuses.length() > 0) log.debug("NSCLIENT received " + devicestatuses.length() + " devicestatuses");
                }
                if (data.has("mbgs")) {
                    JSONArray mbgs = (JSONArray) data.getJSONArray("mbgs");
                    if (mbgs.length() > 0) log.debug("NSCLIENT received " + mbgs.length() + " mbgs");
                    for (Integer index = 0; index < mbgs.length(); index++) {
                    }
                }
                if (data.has("cals")) {
                    JSONArray cals = (JSONArray) data.getJSONArray("cals");
                    if (cals.length() > 0) log.debug("NSCLIENT received " + cals.length() + " cals");
                    // Retreive actual calibration
                    for (Integer index = 0; index < cals.length(); index++) {
                        if (index ==0) {
                            actualCal.set(cals.optJSONObject(index));
                        }
                    }
                }
                if (data.has("sgvs")) {
                    BroadcastSgvs bs = new BroadcastSgvs();
                    String units = MainApp.getNsProfile() != null ? MainApp.getNsProfile().getUnits() : "mg/dl";
                    XDripEmulator emulator = new XDripEmulator();
                    JSONArray sgvs = (JSONArray) data.getJSONArray("sgvs");
                    if (sgvs.length() > 0) log.debug("NSCLIENT received " + sgvs.length() + " sgvs");
                    for (Integer index = 0; index < sgvs.length(); index++) {
                        // log.debug("NSCLIENT svg " + sgvs.getJSONObject(index).toString());
                        NSSgv sgv = new NSSgv(sgvs.getJSONObject(index));
                        // Handle new sgv here
                        if (emulatexDrip) {
                            BgReading bgReading = new BgReading(sgv, actualCal, units);
                            emulator.handleNewBgReading(bgReading, isFull && index == 0, MainApp.instance().getApplicationContext());
                        }
                        bs.handleNewSgv(sgv, MainApp.instance().getApplicationContext(), isDelta);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //log.debug("NSCLIENT onDataUpdate end");
        }
    };

    public void dbUpdate(DbRequest dbr, NSUpdateAck ack) {
        try {
            if (!isConnected) return;
            if (uploading) {
                log.debug("DBUPDATE Busy, adding to queue");
                UploadQueue.put(dbr.hash(), dbr);
                log.debug(UploadQueue.status());
                return;
            }
            uploading = true;
            JSONObject message = new JSONObject();
            message.put("collection", dbr.collection);
            message.put("_id", dbr._id);
            message.put("data", dbr.data);
            mSocket.emit("dbUpdate", message, ack);
            synchronized(ack) {
                try {
                    ack.wait(timeToWaitForResponseInMs);
                } catch (InterruptedException e) {
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        uploading = false;
    }

    public void dbUpdate(DbRequest dbr) {
        try {
            if (!isConnected) return;
            if (uploading) {
                log.debug("DBUPDATE Busy, adding to queue");
                UploadQueue.put(dbr.hash(), dbr);
                log.debug(UploadQueue.status());
                return;
            }
            uploading = true;
            JSONObject message = new JSONObject();
            message.put("collection", dbr.collection);
            message.put("_id", dbr._id);
            message.put("data", dbr.data);
            mSocket.emit("dbUpdate", message);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        uploading = false;
    }

    public void dbUpdateUnset(DbRequest dbr, NSUpdateAck ack) {
        try {
            if (!isConnected) return;
            if (uploading) {
                log.debug("DBUPUNSET Busy, adding to queue");
                UploadQueue.put(dbr.hash(), dbr);
                log.debug(UploadQueue.status());
                return;
            }
            uploading = true;
            JSONObject message = new JSONObject();
            message.put("collection", dbr.collection);
            message.put("_id", dbr._id);
            message.put("data", dbr.data);
            mSocket.emit("dbUpdateUnset", message, ack);
            synchronized(ack) {
                try {
                    ack.wait(timeToWaitForResponseInMs);
                } catch (InterruptedException e) {
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        uploading = false;
    }

    public void dbUpdateUnset(DbRequest dbr) {
        try {
            if (!isConnected) return;
            if (uploading) {
                log.debug("DBUPUNSET Busy, adding to queue");
                UploadQueue.put(dbr.hash(), dbr);
                log.debug(UploadQueue.status());
                return;
            }
            uploading = true;
            JSONObject message = new JSONObject();
            message.put("collection", dbr.collection);
            message.put("_id", dbr._id);
            message.put("data", dbr.data);
            mSocket.emit("dbUpdateUnset", message);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        uploading = false;
    }

    public void dbRemove(DbRequest dbr, NSUpdateAck ack) {
        try {
            if (!isConnected) return;
            if (uploading) {
                log.debug("DBREMOVE Busy, adding to queue");
                UploadQueue.put(dbr.hash(), dbr);
                log.debug(UploadQueue.status());
                return;
            }
            uploading = true;
            JSONObject message = new JSONObject();
            message.put("collection", dbr.collection);
            message.put("_id", dbr._id);
            mSocket.emit("dbRemove", message, ack);
            synchronized(ack) {
                try {
                    ack.wait(timeToWaitForResponseInMs);
                } catch (InterruptedException e) {
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        uploading = false;
    }

    public void dbRemove(DbRequest dbr) {
        try {
            if (!isConnected) return;
            if (uploading) {
                log.debug("DBREMOVE Busy, adding to queue");
                UploadQueue.put(dbr.hash(), dbr);
                log.debug(UploadQueue.status());
                return;
            }
            uploading = true;
            JSONObject message = new JSONObject();
            message.put("collection", dbr.collection);
            message.put("_id", dbr._id);
            mSocket.emit("dbRemove", message);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        uploading = false;
    }

    public void dbAdd(DbRequest dbr, NSAddAck ack) {
        try {
            if (!isConnected) return;
            if (uploading) {
                log.debug("DBADD Busy, adding to queue");
                UploadQueue.put(dbr.hash(), dbr);
                log.debug(UploadQueue.status());
                return;
            }
            uploading = true;
            JSONObject message = new JSONObject();
            message.put("collection", dbr.collection);
            message.put("data", dbr.data);
            mSocket.emit("dbAdd", message, ack);
            synchronized(ack) {
                try {
                    ack.wait(timeToWaitForResponseInMs);
                } catch (InterruptedException e) {
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        uploading = false;
    }

    public void dbAdd(DbRequest dbr) {
        try {
            if (!isConnected) return;
            if (uploading) {
                log.debug("DBADD Busy, adding to queue");
                UploadQueue.put(dbr.hash(), dbr);
                log.debug(UploadQueue.status());
                return;
            }
            uploading = true;
            JSONObject message = new JSONObject();
            message.put("collection", dbr.collection);
            message.put("data", dbr.data);
            mSocket.emit("dbAdd", message);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        uploading = false;
    }

}
