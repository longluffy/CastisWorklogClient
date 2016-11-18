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

public class SignupActivity extends AppCompatActivity implements SendDatatoServer.AsyncResponse{
    Gson gsonParser = new Gson();
    private static final String TAG = "SignupActivity";
    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEditor;
    //    String SIGNUP_URL = "http://192.168.105.104:8080/ciwls/signup";
    String SIGNUP_URL = "/ciwls/signup";
    public static final String DEFAULT_SERVER = "http://110.35.173.28:8886";

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
    @Bind(R.id.link_login)
    TextView _loginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        sharedPref = PreferenceManager
                .getDefaultSharedPreferences(SignupActivity.this);
        sharedPrefEditor = sharedPref.edit();


        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    ProgressDialog progressDialog = null;

    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
        }

        _signupButton.setEnabled(false);

        progressDialog = new ProgressDialog(SignupActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        // TODO: Implement your own signup logic here.

        User signupDTO = new User();
        signupDTO.setFullname(_nameText.getText().toString());
        signupDTO.setEmail(_emailText.getText().toString());
        signupDTO.setUsername(_usernameText.getText().toString());
        signupDTO.setPassword(_passwordText.getText().toString());

//        new HttpAsyncTask().execute(signupDTO);


        String jsonMessage = gsonParser.toJson(signupDTO);
        new SendDatatoServer(this).execute(String.valueOf(jsonMessage), sharedPref.getString("prefServer", DEFAULT_SERVER) + SIGNUP_URL);
    }

    public void onSignupSuccess() {
        _signupButton.setEnabled(true);
        progressDialog.dismiss();

        Toast.makeText(getBaseContext(), "Create Account Success !! Loging in with UserName " + _nameText.getText().toString(), Toast.LENGTH_LONG).show();

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

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
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

        return valid;
    }
    @Override
    public void ServerResponse(String result) {
        Log.i("Server response: ", result);
        User userResponse = gsonParser.fromJson(result, User.class);
        if (result != null) {
            if (userResponse.getStatus() == 0) {
                sharedPrefEditor.putInt("user_id", userResponse.getId());
                sharedPrefEditor.putString("username", userResponse.getUsername());
                sharedPrefEditor.putBoolean("isLoggedIn", true);
                sharedPrefEditor.putString("fullname", userResponse.getFullname());
                sharedPrefEditor.putString("email", userResponse.getEmail());
                sharedPrefEditor.putBoolean("isLoggedIn", true);
                sharedPrefEditor.apply();

                onSignupSuccess();

            } else if (userResponse.getStatus() == 1) {
                _usernameText.setError("Username already exists");
                isEmailFault = false;
                onSignupFailed();

            } else if (userResponse.getStatus() == 2) {
                _emailText.setError("Email already exists");

                isEmailFault = true;
                onSignupFailed();

            }
        } else {
            Toast.makeText(getBaseContext(), "Cannot connect to Server ", Toast.LENGTH_LONG).show();
            onSignupFailed();
        }
    }
}
