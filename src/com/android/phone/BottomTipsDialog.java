package com.android.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import android.view.View;
import android.widget.Button;
import android.os.AsyncTask;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by shaochuanzhi on 12/8/21.
 */

public class BottomTipsDialog extends AlertDialog {
    private Button btn_next;
    private Button btn_clear;

    private TextView tv_mobile_title;
    private TextView tv_mobile_content;

    private Context mContext;
    private OnSkipOrCloseClickListener mOnSkipOrCloseClickListener;
    private boolean mCleanOk = false;
    private boolean theFir = true;
    private String mCommond = "mfg_util --clear_inproductionflag";

    private final String TAG = "BottomTipsDialog";

    public BottomTipsDialog(Context context) {
        super(context, R.style.inproduction_dialog_toast);
        mContext = context;
    }

    public interface OnSkipOrCloseClickListener{
        void onSkipClick(boolean cleanOk);
        void onCloseClick();
    }

    public void setOnSkipOrCloseClickListener(OnSkipOrCloseClickListener onSkipOrCloseClickListener,boolean cleanOk){
        this.mOnSkipOrCloseClickListener = onSkipOrCloseClickListener;
        mCleanOk = cleanOk;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inproduction_mode_flag_clear_dialog);
        setCanceledOnTouchOutside(false);
        initView();
    }

    private void initView() {
        btn_next = (Button) findViewById(R.id.btn_next);
        btn_clear = (Button) findViewById(R.id.btn_clear);

        tv_mobile_title = (TextView) findViewById(R.id.tv_mobile_title);
        tv_mobile_content = (TextView) findViewById(R.id.tv_mobile_content);

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (theFir) {
                    theFir = false;
                    if (mCleanOk) {
                        returnToSuccess();
                    }else{
                       startTask();
                    }
                }else{
                    mOnSkipOrCloseClickListener.onSkipClick(mCleanOk);
                    dismiss();
                }
            }
        });

        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnSkipOrCloseClickListener.onCloseClick();
                dismiss();
            }
        });
    }

    private String runShellCmd(String command) {
        StringBuilder sb = new StringBuilder();
        Runtime r = Runtime.getRuntime();
        Process p;
        try {
            String line;
            android.util.Log.d(TAG, command);
            p = r.exec(command);
            BufferedReader buffReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while( (line = buffReader.readLine()) != null ) {
                sb.append(line);
                android.util.Log.d(TAG, line);
            }
            buffReader.close();
            if(p.waitFor(2, TimeUnit.SECONDS)) {
                android.util.Log.d(TAG, "process exit value="+p.exitValue());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return sb.toString();
    }

    private void startTask(){
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... unused) {

                String result;
                // if (!connect("mfgutil")) {
                //     android.util.Log.e(TAG, "Connecting tracability proxy fail!");
                // } else if (!sendCommand("clear_inproductionflag")) {
                //     android.util.Log.e(TAG, "Send command to tracability proxy fail!");
                // }

                // disconnect();

                result = runShellCmd(mCommond);

                android.util.Log.d(TAG, "result="+result);

                if (result != null && result.contains("cleared")) {

                    return "PASS";
                    
                }else{

                    return "FAIL";

                }
            }

            @Override
            protected void onPostExecute(String result) {
                if ("PASS".equals(result)) {
                    returnToSuccess();                    
                }else{
                    returnToFail();
                }
            }
        }.execute();
    }


    private void returnToSuccess(){
        mCleanOk = true;
        tv_mobile_title.setText(mContext.getResources().getString(R.string.inproduction_mode_flag_clear_success_title));
        tv_mobile_content.setText(mContext.getResources().getString(R.string.inproduction_mode_flag_clear_success_cotent));
        btn_clear.setVisibility(View.GONE);
    }

    private void returnToFail(){
        mCleanOk = false;
        tv_mobile_title.setText(mContext.getResources().getString(R.string.inproduction_mode_flag_clear_fail_title));
        tv_mobile_content.setText(mContext.getResources().getString(R.string.inproduction_mode_flag_clear_fail_cotent));
        btn_clear.setVisibility(View.GONE);
    }

    private LocalSocket mSocket;
    private OutputStream mOut;

    protected boolean connect(String socketName) {
        mSocket = new LocalSocket();
        if (mSocket == null) {
            return false;
        }
        android.util.Log.i(TAG, "connecting...");
        try {
            LocalSocketAddress address = new LocalSocketAddress(
                    socketName, LocalSocketAddress.Namespace.RESERVED);

            mSocket.connect(address);

            mOut = mSocket.getOutputStream();
        } catch (IOException ex) {
            android.util.Log.e(TAG, ex.toString());
            disconnect();
            return false;
        }
        return true;
    }

    protected void disconnect() {
        android.util.Log.i(TAG, "disconnecting...");
        try {
            if (mSocket != null)
                mSocket.close();
        } catch (IOException ex) {
        }
        try {
            if (mOut != null)
                mOut.close();
        } catch (IOException ex) {
        }
        mSocket = null;
        mOut = null;
    }

    protected boolean sendCommand(String cmds) {
        byte[] cmd = cmds.getBytes();
        try {
            if(mOut!=null) {
                mOut.write(cmd);
                mOut.flush();
            }
        } catch (IOException ex) {
            android.util.Log.e(TAG, "write error");
            return false;
        }
        return true;
    }

}
