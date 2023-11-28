package com.example.avr_ballbalancerapp;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class clsIPAddressesListAdapter extends BaseAdapter {

    //INTERFACE
    private IP_ListAdapterInterface ip_listAdapterInterface;

    //Variable Dec
    private ArrayList<clsIPAddresses> mIPAddrItemsList; //List of all fines in Grou
    private Context mContext;
    private LayoutInflater myLayoutInflater;
    private int mLayout;
    private int mSelectedGroup; //e.g. t
    private int touchPos;
    private View rootView;

    private static int lastSelectedPos; //Static Variable - Belongs to class - Used to keep track of which item is pressed

    public clsIPAddressesListAdapter(Context context, ArrayList<clsIPAddresses> arrayList, int layout){
        lastSelectedPos = 0; //Sets it to 0.
        mContext = context;
        mIPAddrItemsList = arrayList;
        myLayoutInflater = LayoutInflater.from(mContext);
        mLayout = layout; //Layout XML Passed In
        touchPos = 0;



    }

    @Override
    public int getCount() {
        return mIPAddrItemsList.size();
    }

    @Override
    public Object getItem(int position) {
        return mIPAddrItemsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final clsIPAddresses oIPAddr = mIPAddrItemsList.get(position);
        final clsViewHolder iViewHolder = new clsViewHolder();


        if(convertView == null)
        {
            convertView = myLayoutInflater.inflate(mLayout, null);

            iViewHolder.tvIPAddrTitle = (TextView) convertView.findViewById(R.id.tvIPAddrTitle);
            iViewHolder.etIPAddrDescription = (EditText)convertView.findViewById(R.id.etDescription);
            iViewHolder.etIPAddrDescription.setTag(position);
            iViewHolder.etIPAddrNr = (EditText)convertView.findViewById(R.id.etIPAddrNr);
            iViewHolder.etIPAddrNr.setTag(position);
            iViewHolder.etPortNr = (EditText)convertView.findViewById(R.id.etPortNr);
            iViewHolder.etPortNr.setTag(position);
            iViewHolder.ivSelectedDrawable = (ImageView) convertView.findViewById(R.id.ivSelectedIcon);

            iViewHolder.tvIPAddrTitle.setText(oIPAddr.getmIP_AddressTitle());
            iViewHolder.etIPAddrDescription.setText(oIPAddr.getmIP_AddressDescription());
            iViewHolder.etIPAddrNr.setText(oIPAddr.getmIP_AddressStr());
            iViewHolder.etPortNr.setText(oIPAddr.getmIP_AddressPORT());

            iViewHolder.ivSelectedDrawable.setImageDrawable(oIPAddr.getServer_IsActiveDrawable());

            iViewHolder.tvIPAddrTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ip_listAdapterInterface.onServerItemClickedEvent(oIPAddr.getmIP_AddressStr()); //returns IPAddress
                }
            });

            //-------------------------------
           iViewHolder.etIPAddrDescription.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    touchPos = position;

                    if (lastSelectedPos != touchPos) {
                        lastSelectedPos = touchPos;
                        Log.i("EditText", "OnTouchListener: iViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressDescription());
                    }
                    return false;
                }
            });

            iViewHolder.etIPAddrDescription.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if ((lastSelectedPos == position) && iViewHolder.etIPAddrDescription.hasFocus()&& (!iViewHolder.etIPAddrDescription.getText().toString().equals(oIPAddr.getmIP_AddressDescription()))) {
                        Log.i("EditText", "afterTextChanged: iViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressDescription());
                        oIPAddr.updateIP_AddressDescription(iViewHolder.etIPAddrDescription.getText().toString());
                    }
                }
            });

            //-------------------------------

            iViewHolder.etIPAddrNr.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    touchPos = position;

                    if (!(lastSelectedPos == touchPos)) {
                        lastSelectedPos = touchPos;
                        Log.i("EditText", "OnTouchListener: iViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressStr());
                    }
                    return false;
                }
            });
            iViewHolder.etIPAddrNr.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if ((lastSelectedPos == position) && iViewHolder.etIPAddrNr.hasFocus() && (!iViewHolder.etIPAddrNr.getText().toString().equals(oIPAddr.getmIP_AddressStr())) ) {
                        Log.i("EditText", "afterTextChanged: iViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressStr());
                        oIPAddr.updateIPAddressStr(iViewHolder.etIPAddrNr.getText().toString());
                    }
                }
            });

            //-------------------------------

            iViewHolder.etPortNr.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    touchPos = position;

                    if (lastSelectedPos != touchPos) {
                        lastSelectedPos = touchPos;
                        Log.i("EditText", "OnTouchListener: iViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressPORT());
                    }
                    return false;
                }
            });

            iViewHolder.etPortNr.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    Log.i("EditText", "OnTextChanged: iViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressPORT());
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if ((lastSelectedPos == position) && iViewHolder.etPortNr.hasFocus() && (!iViewHolder.etPortNr.getText().toString().equals(oIPAddr.getmIP_AddressPORT()))) {
                        Log.i("EditText", "afterTextChanged: iViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressPORT());
                        oIPAddr.updateIPAddressPort(iViewHolder.etPortNr.getText().toString());;
                    }
                }
            });

            iViewHolder.ivSelectedDrawable.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //Ask if user would like to change selected item.
                    Snackbar.make(v, "UPDATE MASTER?", Snackbar.LENGTH_SHORT).setAction("CONFIRM", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            oIPAddr.updateServer_isActive();

                            for (clsIPAddresses tstIP : mIPAddrItemsList) //Updates all mIPAddress items in the list.
                            {
                                tstIP.setServerActiveString("false");
                            }
                            oIPAddr.setServerActiveString("true"); //Set's the modified UPAddress object to "true", as it was selected.
                            notifyDataSetChanged(); //Informs that view shall be updated

                        }
                    }).show();

                    return false;
                }
            });

            //---------------------------------------
            convertView.setTag(iViewHolder);
            this.notifyDataSetChanged();
        }
        else
        {
            final clsIPAddressesListAdapter.clsViewHolder mainViewHolder = (clsIPAddressesListAdapter.clsViewHolder)convertView.getTag();
            //---------------

            mainViewHolder.tvIPAddrTitle =(TextView)convertView.findViewById(R.id.tvIPAddrTitle);
            mainViewHolder.etIPAddrDescription = (EditText)convertView.findViewById(R.id.etDescription);
            mainViewHolder.etIPAddrDescription.setTag(position);
            mainViewHolder.etIPAddrNr = (EditText)convertView.findViewById(R.id.etIPAddrNr);
            mainViewHolder.etIPAddrNr.setTag(position);
            mainViewHolder.etPortNr = (EditText)convertView.findViewById(R.id.etPortNr);
            mainViewHolder.etPortNr.setTag(position);
            mainViewHolder.ivSelectedDrawable = (ImageView) convertView.findViewById(R.id.ivSelectedIcon);

            mainViewHolder.tvIPAddrTitle.setText(oIPAddr.getmIP_AddressTitle());
            mainViewHolder.etIPAddrDescription.setText(oIPAddr.getmIP_AddressDescription());
            mainViewHolder.etIPAddrNr.setText(oIPAddr.getmIP_AddressStr());
            mainViewHolder.etPortNr.setText(oIPAddr.getmIP_AddressPORT());

            mainViewHolder.ivSelectedDrawable.setImageDrawable(oIPAddr.getServer_IsActiveDrawable());
//
            mainViewHolder.tvIPAddrTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ip_listAdapterInterface.onServerItemClickedEvent(oIPAddr.getmIP_AddressStr()); //returns IPAddress
                }
            });

            //--------------------------
            mainViewHolder.etIPAddrDescription.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    touchPos = position;
                    if (lastSelectedPos != touchPos) {
                        lastSelectedPos = touchPos;
                        Log.i("EditText", "OnTouchListener: mainViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressDescription());
                    }
                    return false;
                }
            });

            mainViewHolder.etIPAddrDescription.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if ((lastSelectedPos == position) && mainViewHolder.etIPAddrDescription.hasFocus() && (!mainViewHolder.etIPAddrDescription.getText().toString().equals(oIPAddr.getmIP_AddressDescription()))) {
                        Log.i("EditText", "afterTextChanged: mainViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressDescription());
                        oIPAddr.updateIP_AddressDescription(mainViewHolder.etIPAddrDescription.getText().toString());;
                    }
                }
            });
            //--------------------------

            mainViewHolder.etIPAddrNr.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    touchPos = position;
                    if (lastSelectedPos != touchPos) {
                        lastSelectedPos = touchPos;
                        Log.i("EditText", "OnTouchListener: mainViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressStr());
                    }

                    return false;
                }
            });
            mainViewHolder.etIPAddrNr.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if ((lastSelectedPos == position) && mainViewHolder.etIPAddrNr.hasFocus() && (!mainViewHolder.etIPAddrNr.getText().toString().equals(oIPAddr.getmIP_AddressStr()))) {
                        Log.i("EditText", "afterTextChanged: mainViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressStr());
                        oIPAddr.updateIPAddressStr(mainViewHolder.etIPAddrNr.getText().toString());;
                    }
                }
            });

            //--------------------------
            mainViewHolder.etPortNr.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    touchPos = position;
                    if (lastSelectedPos != touchPos) {
                        lastSelectedPos = touchPos;
                        Log.i("EditText", "OnTouchListener: mainViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressPORT());
                    }
                    return false;
                }
            });

            mainViewHolder.etPortNr.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    Log.i("EditText", "OnTextChanged: mainViewHolder -Pos:" + position + " PORT: " + oIPAddr.getmIP_AddressPORT());
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if ((lastSelectedPos == position) && mainViewHolder.etPortNr.hasFocus() && (!mainViewHolder.etPortNr.getText().toString().equals(oIPAddr.getmIP_AddressPORT()))) {
                        Log.i("EditText", "afterTextChanged: mainViewHolder -Pos:" + position + " Class: " + oIPAddr.getmIP_AddressPORT());
                        oIPAddr.updateIPAddressPort(mainViewHolder.etPortNr.getText().toString());;
                    }
                }
            });
            //--------------------------
            mainViewHolder.ivSelectedDrawable.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //Ask if user would like to change selected item.
                    Snackbar.make(v, "UPDATE MASTER ADDR?", Snackbar.LENGTH_SHORT).setAction("YES", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            oIPAddr.updateServer_isActive(); //Set's all Database servers to "IsActive" = False, and then set's the relevant one to isActive="true".

                            for (clsIPAddresses tstIP : mIPAddrItemsList) //Updates the internal IP_Address objects
                            {
                                tstIP.setServerActiveString("false");
                            }
                            oIPAddr.setServerActiveString("true"); //Set's the modified UPAddress object to "true", as it was selected.
                            notifyDataSetChanged(); //Informs that view shall be updated
                        }
                    }).show();

                    return false;
                }
            });


            //---------------
            convertView.setTag(mainViewHolder);

        }

        return convertView;
    }

    //----------- REQUIRED FOR Adapter -------------
    static class clsViewHolder{

        public TextView tvIPAddrTitle;
        
        public EditText etIPAddrDescription;
        public EditText etIPAddrNr;
        public EditText etPortNr;
        public ImageView ivSelectedDrawable;

    }

    //INTERFACE DEFINITION
    public interface IP_ListAdapterInterface
    {
        public void onServerItemClickedEvent(String ipAddress); //Returns IP_Address
    }

    public void setEventListener(IP_ListAdapterInterface listener) {
        ip_listAdapterInterface = listener;
    }

}
