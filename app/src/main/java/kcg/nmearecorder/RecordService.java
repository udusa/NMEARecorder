package kcg.nmearecorder;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by VLad Landa on 12/19/2016.
 */

public class RecordService extends Service implements GpsStatus.NmeaListener ,LocationListener{


    LocationManager mLocationManager;
    public static boolean IS_SERVICE_RUNNING = false;
    final static String STARTFOREGROUND_ACTION = "STARTFOREGROUND_ACTION";
    final static String STOPFOREGROUND_ACTION = "STOPFOREGROUND_ACTION";
    final static String NMEA_FOLDER = "NMEARecords";
    final static int NOTIFICATION_ID = 9125;

    private  OutputStreamWriter writer;
    private  FileOutputStream fOut;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        if (action.equals(STARTFOREGROUND_ACTION)) {
            String fileName = intent.getStringExtra("filename");
            try {
                writer = getWriter(fileName);
                mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return super.onStartCommand(intent, flags, startId);
                }
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, this);
                mLocationManager.addNmeaListener(this);
                Intent notificationIntent = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

                Intent stopIntent = new Intent(this, RecordService.class);
                stopIntent.setAction(STOPFOREGROUND_ACTION);
                PendingIntent pstopIntent = PendingIntent.getService(this, 0, stopIntent, 0);

                Notification notification = new Notification.Builder(this)
                        .setContentTitle("NMEA Record")
                        .setContentText("Recording to file : "+fileName)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntent)
                        .setTicker("Ticker")
                        .addAction(0, "Stop", pstopIntent)
                        .build();
                startForeground(NOTIFICATION_ID, notification);
            } catch (IOException e) {
                Intent stopIntent = new Intent(this, RecordService.class);
                stopIntent.setAction(STOPFOREGROUND_ACTION);
                startService(stopIntent);
                e.printStackTrace();

            }


        } else if (action.equals(STOPFOREGROUND_ACTION)) {
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onNmeaReceived(long l, String s) {
        try {
            writer.write(s);
        } catch (IOException|NullPointerException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onDestroy() {
        Log.d("Destroyed","Destroyed");
        try {
            mLocationManager.removeNmeaListener(this);
        }catch (RuntimeException e){}
        try {
            writer.close();
        } catch (IOException|NullPointerException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException|NullPointerException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    OutputStreamWriter getWriter(String fileName) throws IOException {
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + NMEA_FOLDER);
/*        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if(isSDPresent) {
        }*/
        folder.mkdir();
        if (folder.exists()) {
            File file = new File(folder, fileName);
            if(file.createNewFile()){
                fOut = new FileOutputStream(file);
                return new OutputStreamWriter(fOut);
            }
        }
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
