package info.nightscout.nsupload;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static org.slf4j.Logger log = LoggerFactory.getLogger(MainActivity.class);
    static Handler handler;
    static private HandlerThread handlerThread;
    private TextView mTextView;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(handler==null) {
            handlerThread = new HandlerThread(MainActivity.class.getSimpleName() + "Handler");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.post(new Runnable() {
                                 @Override
                                 public void run() {
                                     log.debug("Hello");
                                 }
                             });
            }
        });

        mTextView = (TextView) findViewById(R.id.log);

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLoggerList();
        Logger logger = (Logger) LoggerFactory.getLogger("ROOT");
        TextViewLogger newAppender = new TextViewLogger(mTextView, new Handler(Looper.getMainLooper()));
        newAppender.setContext(lc);
        newAppender.start();
        logger.addAppender(newAppender);

        logger.setLevel(Level.DEBUG);
        logger.setAdditive(true);



    }

    private void log(final String s) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.append(timeFormat.format(new Date()) + " " + s + "\n");
            }
        });

    }

    public static class TextViewLogger extends AppenderBase<ILoggingEvent> {
        private final Handler handler;
        private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        private TextView mTextView;

        public TextViewLogger(TextView mTextView, Handler handler) {
            this.mTextView = mTextView;
            this.handler = handler;
        }


        @Override
        protected void append(final ILoggingEvent eventObject) {
            if(mTextView!=null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.append(timeFormat.format(new Date()) + " " + eventObject.getMessage() + "\n");
                    }
                });


            }
        }
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}
