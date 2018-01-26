package net.aaronjamt.remotecamera;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.PointerIcon;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

public class Main extends AppCompatActivity {
    private static final String TAG = "Main";
    private SectionsPageAdapter mSectionsPageAdapter;
    private ViewPager mViewPager;
    ArrayAdapter<String> adapter;
    ArrayList<String> camerasAvalible=new ArrayList<>();
    android.net.wifi.WifiManager.MulticastLock lock;
    private String type = "_remotecam._udp.";
    Boolean serverState = false;
    JmDNS jmdns;

    private void setUp() {
        try {
            WifiManager wifi =
                    (WifiManager)
                            getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            assert wifi != null;
            lock = wifi.createMulticastLock(getClass().getName());
            lock.setReferenceCounted(true);
            lock.acquire();
            jmdns = JmDNS.create(getDeviceIpAddress(wifi), "RemoteCamera");
            final JmDNS finalJmdns = jmdns;
            jmdns.addServiceListener(type, new ServiceListener() {
                public void serviceResolved(ServiceEvent ev) {
                    System.out.println("Service resolved: "
                            + ev.getInfo().getQualifiedName()
                            + " port:" + ev.getInfo().getPort());
                    camerasAvalible.add(ev.getName());
                }

                public void serviceRemoved(ServiceEvent ev) {
                    System.out.println("Service removed: " + ev.getName());
                    Iterator itr = camerasAvalible.iterator();
                    while (itr.hasNext())
                    {
                        String x = (String)itr.next();
                        if (x.equals(ev.getName())) itr.remove(); break;
                    }
                }

                public void serviceAdded(ServiceEvent event) {
                    // Required to force serviceResolved to be called again
                    // (after the first search)
                    finalJmdns.requestServiceInfo(event.getType(), event.getName(), 1);
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.container);
        setupViewPager(mViewPager);
    }

    void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new tab1_client(), "Client");
        adapter.addFragment(new tab2_server(), "Server");
        viewPager.setAdapter(adapter);
    }

    public void onToggleClicked(View view) {
        ImageButton button = view.findViewById(R.id.imageButton);
        int icon;
        if (serverState) {
            serverState = false;
            icon = R.drawable.off;
        } else {
            serverState = true;
            icon = R.drawable.on;
        }
        button.setBackgroundResource(icon);
        changeServerStatus(serverState);
    }
    void changeServerStatus(boolean state) {
        if (state) {
            System.out.println("On");
        } else {
            System.out.println("Off");
        }
    }

    private InetAddress getDeviceIpAddress(WifiManager wifi) {
        InetAddress result = null;
        try {
            // default to Android localhost
            result = InetAddress.getByName("10.0.0.2");

            // figure out our wifi address, otherwise bail
            WifiInfo wifiinfo = wifi.getConnectionInfo();
            int intaddr = wifiinfo.getIpAddress();
            byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
                    (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
            result = InetAddress.getByAddress(byteaddr);
        } catch (UnknownHostException ex) {
            Log.w(TAG, String.format("getDeviceIpAddress Error: %s", ex.getMessage()));
        }

        return result;
    }

    protected void onStop() {
        super.onStop();
        stopScan();
    }

    private void stopScan() {
        try {
            if (jmdns != null) {
                Log.i(TAG, "Stopping ZeroConf probe....");
                jmdns.unregisterAllServices();
                jmdns.close();
                jmdns = null;
            }
            if (lock != null) {
                Log.i(TAG, "Releasing Mutlicast Lock...");
                lock.release();
                lock = null;
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }
    }
}
