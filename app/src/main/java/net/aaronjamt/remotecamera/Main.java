package net.aaronjamt.remotecamera;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
    private String type = "_remoteCam._tcp.local.";

    private void setUp() {
        try {
            WifiManager wifi =
                    (WifiManager)
                            getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            lock = wifi.createMulticastLock("HeeereDnssdLock");
            lock.setReferenceCounted(true);
            lock.acquire();
            JmDNS jmdns = JmDNS.create();
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
                        if (x == ev.getName()) itr.remove(); break;
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

    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new tab1_client(), "Client");
        adapter.addFragment(new tab2_server(), "Server");
        viewPager.setAdapter(adapter);
    }
}
