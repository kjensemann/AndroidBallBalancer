package com.example.avr_ballbalancerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class settingsActivity extends AppCompatActivity {

    //FIREBASE
    private FirebaseDatabase mFbDb;
    private DatabaseReference mFbDbRefMain;
    private DatabaseReference mFbDbRefServers;

    private Context mContext;

    private ListView lv_IPAddresses;

    private ArrayList<clsIPAddresses> lstIpAddressesArrayList = new ArrayList<>();
    private clsIPAddressesListAdapter ipAddressesListAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settingsactivity);

        mContext = getApplicationContext();

        lv_IPAddresses = (ListView)findViewById(R.id.lv_ipAddresses);
        ipAddressesListAdapter = new clsIPAddressesListAdapter(this,lstIpAddressesArrayList,R.layout.layout_ipaddresslistadapter);

        ipAddressesListAdapter.setEventListener(new clsIPAddressesListAdapter.IP_ListAdapterInterface() {
            @Override
            public void onServerItemClickedEvent(String ipAddress) {
                Toast.makeText(mContext, ipAddress + " CLICKED", Toast.LENGTH_SHORT).show();
                mFbDbRefMain = mFbDb.getReference();
                mFbDbRefMain.child("ServerIP_AddrSelected").setValue(ipAddress);

            }
        });

        //FIREBASE
        mFbDb = FirebaseDatabase.getInstance();
        mFbDbRefServers = mFbDb.getReference().child("Servers");
        //mFbDbRefServers.child("LogOnMessage").setValue("Logged On: ");

        mFbDbRefServers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    clsIPAddresses tmpIPAddress = new clsIPAddresses(mContext);

                    tmpIPAddress.setmIP_AddressTitle(snapshot.getKey());
                    tmpIPAddress.setmFbDbServerKey(snapshot.getKey()); //Eg. Server1
                    tmpIPAddress.setmIP_AddressStr(snapshot.child("IP_Addr").getValue().toString());
                    tmpIPAddress.setmIP_AddressPORT(snapshot.child("PortNr").getValue().toString());
                    tmpIPAddress.setmIP_AddressDescription(snapshot.child("ServerName").getValue().toString());
                    tmpIPAddress.setServerActiveString(snapshot.child("IsSelected").getValue().toString());

                    lstIpAddressesArrayList.add(tmpIPAddress);
                    lv_IPAddresses.setAdapter(ipAddressesListAdapter);


                }

            }



            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}