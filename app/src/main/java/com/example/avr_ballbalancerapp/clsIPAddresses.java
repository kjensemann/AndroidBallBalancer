package com.example.avr_ballbalancerapp;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class clsIPAddresses {

    private String mFbDbServerKey;
    private String mIP_AddressStr;
    private String mIP_AddressDescription;
    private String mIP_AddressTitle;
    private String mIP_AddressPORT;
    private int server_isActive; //0=No, 1=Yes

    private Drawable server_IsActiveDrawable; //

    //FIREBASE
    private FirebaseDatabase mFbDb;
    private DatabaseReference mFbDbRefMain;
    private DatabaseReference mFbDbRefServers;

    private Context mContext;

    public clsIPAddresses(Context context){

        //FIREBASE
        mFbDb = FirebaseDatabase.getInstance();
        mFbDbRefServers = mFbDb.getReference().child("Servers");
        mContext = context;

    }

    public String getmFbDbServerKey() {
        return mFbDbServerKey;
    }
    public void setmFbDbServerKey(String mFbDbServerKey) {
        this.mFbDbServerKey = mFbDbServerKey; //e.g. Server1, Server2 etc
    }

    public String getmIP_AddressStr() {
        return mIP_AddressStr;    }

    public void setmIP_AddressStr(String mIP_AddressStr) {
        this.mIP_AddressStr = mIP_AddressStr;
    }

    public void updateIPAddressStr(String value)
    {
        mFbDbRefServers.child(mFbDbServerKey).child("IP_Addr").setValue(value);
        mIP_AddressStr=value;

    }

    public String getmIP_AddressDescription() {
        return mIP_AddressDescription;
    }
    public void setmIP_AddressDescription(String mIP_AddressDescription) {
        this.mIP_AddressDescription = mIP_AddressDescription;
    }
    public void updateIP_AddressDescription(String value)
    {
        mFbDbRefServers.child(mFbDbServerKey).child("ServerName").setValue(value);
        mIP_AddressDescription=value;

    }

    public String getmIP_AddressTitle() {
        return mIP_AddressTitle;
    }
    public void setmIP_AddressTitle(String mIP_AddressTitle) {
        this.mIP_AddressTitle = mIP_AddressTitle;
    }

    public String getmIP_AddressPORT() {
        return mIP_AddressPORT;
    }
    public void setmIP_AddressPORT(String mIP_AddressPORT) {
        this.mIP_AddressPORT = mIP_AddressPORT;
    }
    public void updateIPAddressPort(String value)
    {
        mFbDbRefServers.child(mFbDbServerKey).child("PortNr").setValue(value);
        mIP_AddressPORT=value;

    }

    public int getServer_isActive() {
        return server_isActive;
    }

    public void setServerActiveString(String val){
        if (val.equals("true")){
            this.server_isActive = 1;
        }
        else {
            this.server_isActive = 0;
        }

    }
    public void updateServer_isActive() {

        //Initially all are set to false
        mFbDbRefServers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot sh : snapshot.getChildren()){ //Sets all to status "false"

                    mFbDbRefServers.child(sh.getKey()).child("IsSelected").setValue("false");

                }

                //set's selected mFbDbServerKey to "isActive".
                mFbDbRefServers.child(mFbDbServerKey).child("IsSelected").setValue("true"); //Updates value

            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        this.server_isActive = 1;

    }
    public Drawable getServer_IsActiveDrawable()
    {
        if (this.server_isActive == 1)
        {
            server_IsActiveDrawable = AppCompatResources.getDrawable(mContext,R.drawable.fbdb_plain_512x512);
        }
        else {
            server_IsActiveDrawable = AppCompatResources.getDrawable(mContext, R.drawable.description24px_png);
        }
        return server_IsActiveDrawable;
    }
}
