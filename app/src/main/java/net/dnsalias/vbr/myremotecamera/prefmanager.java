package net.dnsalias.vbr.myremotecamera;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


/**
 * Created by fr20033 on 20/03/2015.
 */
public class prefmanager {
    // Shared Preferences    
    SharedPreferences pref;

    // Editor for Shared preferences    
    Editor editor;

    // Context    
    Context _context;

    // Sharedpref file name    
    private static final String PREF_NAME = "MyRemoteCameraPref";

    // User name (make variable public to access from outside)
    public static final String KEY_PORT = "port";

    // Email address (make variable public to access from outside)
    public static final String KEY_SERVER = "server";

    // Constructor
    public prefmanager(Context context){
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
      * Create server connection
    * */
    public void createServer(String server, int port) {
        editor.putString(KEY_SERVER, server);
        editor.putInt(KEY_PORT, port);
        editor.commit();
    }

    /**
      * Get stored data
      **/
    public String getServerName() {
        return pref.getString(KEY_SERVER, null);
    }

    public int getServerPort() {
        return pref.getInt(KEY_PORT, -1);
    }

}
