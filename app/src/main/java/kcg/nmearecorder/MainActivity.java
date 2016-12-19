package kcg.nmearecorder;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity implements LocationListener, GpsStatus.NmeaListener {

    TextView nmeaDataView;
    Button recordBtn;
    LocationManager mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nmeaDataView = (TextView) findViewById(R.id.nmeaDataView);
        nmeaDataView.setMovementMethod(new ScrollingMovementMethod());

        recordBtn = (Button)findViewById(R.id.recordBtn);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 10, this);

    }

    @Override
    protected void onResume() {

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            enableLocationDialog();
        }else{
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 10, this);
            mLocationManager.addNmeaListener(this);
        }

        if(isServiceRunningInForeground(getApplicationContext(),RecordService.class)){
            recordBtn.setText("Stop Recording");
        }

        super.onResume();
    }

    @Override
    public void onNmeaReceived(long l, String s) {
        nmeaDataView.append(s+"\n");
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

    public void recordBtnClick(View v){
        if(recordBtn.getText().equals("Start Recording")) {
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                fileNameDialog();
            } else {
                Toast.makeText(getApplicationContext(), "Location not enabled!", Toast.LENGTH_SHORT).show();
            }
        }else{
            Intent service  = new Intent(getApplicationContext(),RecordService.class);
            service.setAction(RecordService.STOPFOREGROUND_ACTION);
            startService(service);
            recordBtn.setText("Start Recording");
        }

    }

    public String isEndWithTxt(String s){
        String[] splits = s.split(".");
        if(splits[splits.length-1].equals("txt")){
            return s;
        }
        return s+".txt";
    }

    public void fileNameDialog(){
        final EditText txtUrl = new EditText(this);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date now = new Date();
        final String fileName = "NMEA_"+formatter.format(now)+".txt";
        txtUrl.setText(fileName);
        new AlertDialog.Builder(this)
                .setTitle("File")
                .setMessage("Set file name to save NMEA data.")
                .setView(txtUrl)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent service  = new Intent(getApplicationContext(),RecordService.class);
                        //service.putExtra("filename",isEndWithTxt(txtUrl.getText().toString()));
                        service.putExtra("filename",fileName);
                        service.setAction(RecordService.STARTFOREGROUND_ACTION);
                        startService(service);
                        recordBtn.setText("Stop Recording");
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                })
                .show();
    }

    public void enableLocationDialog(){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Enable GPS");
        alertDialog.setMessage("GPS is disabled in your device. Would you like to enable it?");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent callGPSSettingIntent = new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Exit",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }

    public static boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    public void openFolderClick(View v){
        if(recordBtn.getText().equals("Stop Recording")){
            recordBtn.performClick();
        }
/*        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        File folder = new File(Environment.getExternalStorageDirectory().getPath() +
                "/"+ RecordService.NMEA_FOLDER+"/");
        Uri uri = Uri.fromFile(folder);
        //String type = folder.toString()+"*//*";
        Log.d("Folder",uri.toString());
        //intent.setType(type);
        intent.setData(uri);
        intent.setType("**//*");
        startActivity(intent);
*/
        openFolder();
    }

    public void openFolder()
    {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/"+RecordService.NMEA_FOLDER+"/");
//        Intent intent = new Intent(Intent.ACTION_VIEW);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(Uri.fromFile(file), "*/*");
        startActivity(Intent.createChooser(intent, getString(R.string.folder_open)));
    }

}
