package com.castis.castisworklogclient.View;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.castis.castisworklogclient.Presenter.SendDatatoServer;
import com.castis.castisworklogclient.R;
import com.castis.castisworklogclient.model.User;
import com.google.gson.Gson;

import butterknife.Bind;
import butterknife.ButterKnife;

public class EditActivity extends AppCompatActivity implements SendDatatoServer.AsyncResponse {
    Gson gsonParser = new Gson();
    private static final String TAG = "EditActivity";
    String SAVE_URL = "/ciwls/user/";
    public static final String DEFAULT_SERVER = "http://110.35.173.28:8886";
    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEditor;

    Boolean isEmailFault = false;

    @Bind(R.id.input_name)
    EditText _nameText;
    @Bind(R.id.input_email)
    EditText _emailText;
    @Bind(R.id.input_username)
    EditText _usernameText;
    @Bind(R.id.input_password)
    EditText _passwordText;
    @Bind(R.id.input_reEnterPassword)
    EditText _reEnterPasswordText;
    @Bind(R.id.btn_signup)
    Button _signupButton;
    @Bind(R.id.link_main)
    TextView _loginLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        ButterKnife.bind(this);

        //initiate Android Shared preference
        sharedPref = PreferenceManager
                .getDefaultSharedPreferences(EditActivity.this);
        sharedPrefEditor = sharedPref.edit();

        _nameText.setText(sharedPref.getString("fullname", ""));
        _usernameText.setText(sharedPref.getString("username", ""));
        _emailText.setText(sharedPref.getString("email", ""));
        _passwordText.setText(sharedPref.getString("password", ""));
        _reEnterPasswordText.setText(sharedPref.getString("password", ""));

        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAccount();
            }
        });

        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    ProgressDialog progressDialog = null;

    public void saveAccount() {
        Log.d(TAG, "Save");

        progressDialog = new ProgressDialog(EditActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Saving Account...");
        progressDialog.show();
        if (!validate()) {
            onSignupFailed();
            return;
        }

        _signupButton.setEnabled(false);

        User signupDTO = new User();
        signupDTO.setId(sharedPref.getInt("user_id", 0));
        signupDTO.setFullname(_nameText.getText().toString());
        signupDTO.setEmail(_emailText.getText().toString().trim());
        signupDTO.setUsername(_usernameText.getText().toString().trim());

        if (_passwordText.getText().toString().trim().equals("")) {
            signupDTO.setPassword(sharedPref.getString("password", "castis"));
        } else {
            signupDTO.setPassword(_passwordText.getText().toString().trim());
        }

        String jsonMessage = gsonParser.toJson(signupDTO);
        new SendDatatoServer(this).execute(String.valueOf(jsonMessage), sharedPref.getString("prefServer", DEFAULT_SERVER) + SAVE_URL+"/"+signupDTO.getId());
    }



    public void onSignupSuccess() {
        _signupButton.setEnabled(true);
        progressDialog.dismiss();

        Toast.makeText(getBaseContext(), "Save Account Success !! " + _nameText.getText().toString(), Toast.LENGTH_LONG).show();

        setResult(RESULT_OK, null);
        finish();
    }

    public void onSignupFailed() {
        progressDialog.dismiss();

        Toast.makeText(getBaseContext(), "SignUp failed", Toast.LENGTH_LONG).show();

        _signupButton.setEnabled(true);

        if (isEmailFault) {
            _emailText.requestFocus();
        } else {
            _usernameText.requestFocus();
        }
    }


    public boolean validate() {
        boolean valid = true;

        String name = _nameText.getText().toString();
        String username = _usernameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();
        String reEnterPassword = _reEnterPasswordText.getText().toString();

        if (name.isEmpty() || name.length() < 3) {
            _nameText.setError("at least 3 characters");
            valid = false;
        } else {
            _nameText.setError(null);
        }

        if (username.isEmpty()) {
            _usernameText.setError("Enter Valid Address");
            valid = false;
        } else {
            _usernameText.setError(null);
        }


        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email username");
            valid = false;
        } else {
            _emailText.setError(null);
        }


        if (password.isEmpty() && reEnterPassword.isEmpty()) {

            //do nothing because password will be unchanged

        } else {

            if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
                _passwordText.setError("between 4 and 10 alphanumeric characters");
                valid = false;
            } else {
                _passwordText.setError(null);
            }

            if (reEnterPassword.isEmpty() || reEnterPassword.length() < 4 || reEnterPassword.length() > 10 || !(reEnterPassword.equals(password))) {
                _reEnterPasswordText.setError("Password Do not match");
                valid = false;
            } else {
                _reEnterPasswordText.setError(null);
            }

        }
        return valid;
    }
    @Override
    public void ServerResponse(String result) {
        User userResponse = gsonParser.fromJson(result, User.class);
        if (result != null) {
            if (userResponse.getStatus() == 0) {

                sharedPrefEditor.putString("username", userResponse.getUsername());
                sharedPrefEditor.putString("fullname", userResponse.getFullname());
                sharedPrefEditor.putString("email", userResponse.getEmail());
                sharedPrefEditor.commit();
                onSignupSuccess();

            } else if (userResponse.getStatus() == 1) {
                _usernameText.setError("Username already exists");
                isEmailFault = false;
                onSignupFailed();
                return;

            } else if (userResponse.getStatus() == 2) {
                _emailText.setError("Email already exists");

                isEmailFault = true;
                onSignupFailed();
                return;

            }
        } else {
            Toast.makeText(getBaseContext(), "Cannot connect to Server ", Toast.LENGTH_LONG).show();
            onSignupFailed();
            return;
        }
    }


}