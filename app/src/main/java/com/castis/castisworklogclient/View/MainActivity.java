package com.castis.castisworklogclient.View;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.castis.castisworklogclient.Presenter.HttpHandler;
import com.castis.castisworklogclient.R;
import com.castis.castisworklogclient.model.Worklog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.identity.intents.Address;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, LocationListener {
    private static final String TAG = "MainActivity";
    Location mLastLocation = null;
    Location mCurrentLocation = new Location("");
    private double fusedLatitude = 0.0;
    private double fusedLongitude = 0.0;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEditor;
    public static final String CHECKIN_URL = "/ciwls/checkin";
    public static final String CHECKOUT_URL = "/ciwls/checkout";
    //    public static final String DEFAULT_SERVER = "http://192.168.105.143:8080";
    public static final String DEFAULT_SERVER = "http://110.35.173.28:8886";

    DecimalFormat df = new DecimalFormat("#.##");


    @Bind(R.id.locationText)
    TextView _locationText;

    @Bind(R.id.distanceText)
    TextView _distanceText;

    @Bind(R.id.checkBtn)
    Button _submitCheckBtn;


    @Bind(R.id.workcontentText)
    EditText _workContentText;

//    @Bind(R.id.greetingText)
//    TextView _greetingText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //initiate Android Shared preference
        sharedPref = getSharedPreferences("TmsLoginState", Context.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();

        //check logged in or not ?
        boolean islogged = sharedPref.getBoolean("isLoggedIn", false);
        if (!islogged) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else {
//            _greetingText.setText("Hi " + sharedPref.getString("username", "") + " ! ");
            Toast.makeText(getBaseContext(), "Create Account Success !! Loging in with UserName " + sharedPref.getString("username", ""), Toast.LENGTH_SHORT).show();

        }

        //check is checkin or not?
        boolean isCheckedIn = sharedPref.getBoolean("isCheckedIn", false);
        if (!isCheckedIn)
            _submitCheckBtn.setText("CheckIN");
        else
            _submitCheckBtn.setText("CheckOUT");


        //check permission to get Location
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    2);
        }

        //Start google play Services
        if (checkPlayServices()) {
            startFusedLocation();
            registerRequestUpdate(this);
        }

        _submitCheckBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (_workContentText.getText().toString().equals("") && sharedPref.getBoolean("isCheckedIn", false) == true) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Empty Work Contents")
                            .setMessage("Your work contents is empty , Are you sure you want to Checkout?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    submitCheck();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing - cobtinue to edit
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    submitCheck();
                }
            }
        });

    }


    public void submitCheck() {
        double distanceToCompany = getDistance();
        boolean isCheckedIn = sharedPref.getBoolean("isCheckedIn", false);

        if (distanceToCompany > 250) {

            Toast.makeText(getBaseContext(), "You're not nearby the office(" + df.format(distanceToCompany) + "m away)", Toast.LENGTH_LONG).show();
        } else if (isCheckedIn) {
            //nearby , Checkin
            CheckOut();
        } else {
            CheckIn();
        }

    }


    ProgressDialog progressDialog = null;

    private void CheckOut() {
        Worklog checkOutDTO = new Worklog();
        checkOutDTO.setId(sharedPref.getInt("user_id", -1));
        checkOutDTO.setLocation(mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude());
        checkOutDTO.setWorkSummary(_workContentText.getText().toString());
        progressDialog = new ProgressDialog(MainActivity.this,
                R.style.AppTheme_Dark_Dialog);

        Log.d(TAG, "Checkin");

        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Connecting...");
        progressDialog.show();

        new CheckOutAsyncTask().execute(checkOutDTO);
    }

    private class CheckOutAsyncTask extends AsyncTask<Worklog, Void, Worklog> {
        @Override
        protected Worklog doInBackground(Worklog... logoutDTO) {
            return POST(sharedPref.getString("server", DEFAULT_SERVER) + CHECKOUT_URL, logoutDTO[0]);
        }

        @Override
        protected void onPostExecute(Worklog checkOutResult) {
            progressDialog.dismiss();

            if (null == checkOutResult || (checkOutResult.getResult() != 0)) {
                //Checkout Fail
                Toast.makeText(getBaseContext(), "CheckOut Failed", Toast.LENGTH_LONG).show();
            } else {
                //Checkout success
                sharedPrefEditor.putBoolean("isCheckedIn", false);
                sharedPrefEditor.apply();
                _submitCheckBtn.setText("CheckIN");
                Toast.makeText(getBaseContext(), "CheckOut Success", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void CheckIn() {

        Worklog checkInDTO = new Worklog();
        checkInDTO.setId(sharedPref.getInt("user_id", -1));
        checkInDTO.setLocation(mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude());
        progressDialog = new ProgressDialog(MainActivity.this,
                R.style.AppTheme_Dark_Dialog);

        Log.d(TAG, "Checkin");

        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Connecting...");
        progressDialog.show();

        new CheckInAsyncTask().execute(checkInDTO);


    }

    private class CheckInAsyncTask extends AsyncTask<Worklog, Void, Worklog> {
        @Override
        protected Worklog doInBackground(Worklog... loginDTO) {
            return POST(sharedPref.getString("server", DEFAULT_SERVER) + CHECKIN_URL, loginDTO[0]);
        }

        // onPostExecute control the results of the AsyncTask.
        @Override
        protected void onPostExecute(Worklog checkInResult) {
            progressDialog.dismiss();

            if (null != checkInResult && checkInResult.getResult() == 0) {
                //Checkin success
                sharedPrefEditor.putBoolean("isCheckedIn", true);
                sharedPrefEditor.putString("LastCheckinLocation", checkInResult.getLocation());
                sharedPrefEditor.apply();

                _submitCheckBtn.setText("CheckOUT");
                Toast.makeText(getBaseContext(), "Checkin Success", Toast.LENGTH_LONG).show();

            } else {
                //Checkin Fail
                Toast.makeText(getBaseContext(), "Checkin Failed", Toast.LENGTH_LONG).show();
            }
        }
    }


    public Worklog POST(String url, Worklog person) {
        InputStream inputStream;
        Worklog parsedResult = null;
        try {
            HttpHandler httpHandler = new HttpHandler();
            inputStream = httpHandler.requestAPI(url, person);

            // convert inputstream to LoginDTO
            parsedResult = convertInputStreamToDTO(inputStream);
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }
        return parsedResult;
    }


    private Worklog convertInputStreamToDTO(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        Gson gsonParser = new Gson();
        Worklog parsedDTO = gsonParser.fromJson(bufferedReader, Worklog.class);

        return parsedDTO;
    }

    public double getDistance() {
        Location companyPVI = new Location("");
        companyPVI.setLatitude(21.0240667);
        companyPVI.setLongitude(105.7867607);

        return mCurrentLocation.distanceTo(companyPVI);


    }

    public void registerRequestUpdate(final LocationListener listener) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000); // every second

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mGoogleApiClient.isConnected())
                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, listener);
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!isGoogleApiClientConnected()) {
                        mGoogleApiClient.connect();
                    }
                    registerRequestUpdate(listener);
                }
            }
        }, 1000);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Toast.makeText(getApplicationContext(),
                        "This device is supported. Please download google play services", Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }


    public boolean isGoogleApiClientConnected() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }


    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
        setFusedLatitude(location.getLatitude());
        setFusedLongitude(location.getLongitude());
        mCurrentLocation = location;
        _locationText.setText(getFusedLatitude() + "," + getFusedLongitude());

        List<Address> addresses = null;

        _distanceText.setText(String.valueOf(df.format(getDistance())));

    }


    public void setFusedLatitude(double lat) {
        fusedLatitude = lat;
    }

    public void setFusedLongitude(double lon) {
        fusedLongitude = lon;
    }

    public double getFusedLatitude() {
        return fusedLatitude;
    }

    public double getFusedLongitude() {
        return fusedLongitude;
    }

    public void startFusedLocation() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnectionSuspended(int cause) {
                            Toast.makeText(getBaseContext(), "Connection to Google API Suspended", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onConnected(Bundle connectionHint) {
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions((Activity) MainActivity.this,
                                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                        2);
                            }

                            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                            if (mLastLocation != null) {
                                _locationText.setText(String.valueOf(mLastLocation.getLatitude()) + " , " + String.valueOf(mLastLocation.getLongitude()));
                            }
                        }
                    }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {

                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            Toast.makeText(getBaseContext(), "Connection to Google API Failed", Toast.LENGTH_SHORT).show();

                        }

                    }).build();
            mGoogleApiClient.connect();
        } else {
            mGoogleApiClient.connect();
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.logout) {
            Toast.makeText(MainActivity.this, "Logout Success", Toast.LENGTH_SHORT).show();
            sharedPrefEditor.remove("isLoggedIn");
            sharedPrefEditor.remove("user_id");
            sharedPrefEditor.commit();

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.edit_UserInfo) {
            Intent intent = new Intent(this, EditActivity.class);
            startActivity(intent);

        } else if (id == R.id.settings) {
            Toast.makeText(this.getApplicationContext(), "this is for settings", Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}