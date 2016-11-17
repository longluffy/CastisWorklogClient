package com.castis.castisworklogclient.View;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.castis.castisworklogclient.Presenter.HttpHandler;
import com.castis.castisworklogclient.R;
import com.castis.castisworklogclient.model.User;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEditor;
    String loginUrl = "/ciwls/login";
    //        public static final String DEFAULT_SERVER = "http://192.168.105.104:8080";
    public static final String DEFAULT_SERVER = "http://110.35.173.28:8886";


    @Bind(R.id.input_username)
    EditText _usernameText;
    @Bind(R.id.input_password)
    EditText _passwordText;
    @Bind(R.id.btn_login)
    Button _loginButton;
    @Bind(R.id.link_signup)
    TextView _signupLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        sharedPref = PreferenceManager
                .getDefaultSharedPreferences(LoginActivity.this);
        sharedPrefEditor = sharedPref.edit();

        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
                finish();
            }
        });
    }

    ProgressDialog progressDialog = null;

    public void login() {
        progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);

        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        _loginButton.setEnabled(false);

        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        User logindto = new User();
        logindto.setUsername(_usernameText.getText().toString());
        logindto.setPassword(_passwordText.getText().toString());


        new HttpAsyncTask().execute(logindto);

    }

    public static User POST(String url, User person) {
        InputStream inputStream;
        User parsedResult = null;
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


    private class HttpAsyncTask extends AsyncTask<User, Void, User> {
        @Override
        protected User doInBackground(User... loginDTO) {
            return POST(sharedPref.getString("prefServer", DEFAULT_SERVER) + loginUrl, loginDTO[0]);
        }

        // onPostExecute control the results of the AsyncTask.
        @Override
        protected void onPostExecute(User result) {
            if (result != null) {
                if ((result.getStatus() == 0) || (result.getStatus() == 1)) {

                    sharedPrefEditor.putInt("user_id", result.getId());
                    sharedPrefEditor.putString("username", result.getUsername());
                    sharedPrefEditor.putString("fullname", result.getFullname());
                    sharedPrefEditor.putString("email", result.getEmail());
                    sharedPrefEditor.putBoolean("isLoggedIn", true);
                    if (result.getStatus() == 0)
                        sharedPrefEditor.putBoolean("isCheckedIn", false);
                    else
                        sharedPrefEditor.putBoolean("isCheckedIn", true);

                    sharedPrefEditor.apply();

                    onLoginSuccess();
                } else {
                    //login failed
                    onLoginFailed();
                }
            } else {
                Toast.makeText(getBaseContext(), "Cannot connect to Server ", Toast.LENGTH_LONG).show();
                onLoginFailed();
            }
        }
    }

    private static User convertInputStreamToDTO(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        Gson gsonParser = new Gson();
        User parsedResult = gsonParser.fromJson(bufferedReader, User.class);
        inputStream.close();
        return parsedResult;

    }

    public void onLoginSuccess() {
        progressDialog.dismiss();

        _loginButton.setEnabled(true);

        Intent main = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(main);
        finish();
    }

    public void onLoginFailed() {
        progressDialog.dismiss();

        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        _loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String username = _usernameText.getText().toString();
        String password = _passwordText.getText().toString();

        // TODO add validate

        if (username.isEmpty() || username.length() < 4 || username.length() > 20) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _usernameText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 20) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onSettingServer(View view) {
        Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
        startActivity(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {

                // TODO: Implement successful signup logic here
                this.finish();
            }
        }
    }

}
