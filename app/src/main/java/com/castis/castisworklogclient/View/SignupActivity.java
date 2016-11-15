package com.castis.castisworklogclient.View;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.io.InputStream;
import java.io.InputStreamReader;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";
//    String SIGNUP_URL = "http://192.168.105.143:8080/ciwls/signup";
    String SIGNUP_URL = "http://110.35.173.28:8886/ciwls/signup";



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
            return;
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

        new HttpAsyncTask().execute(signupDTO);

    }


    private class HttpAsyncTask extends AsyncTask<User, Void, User> {
        @Override
        protected User doInBackground(User... signupDTOs) {
            return POST(SIGNUP_URL, signupDTOs[0]);
        }

        // onPostExecute control the results of the AsyncTask.
        @Override
        protected void onPostExecute(User result) {
            if (result != null) {
                if (result.getStatus()==0) {
                    System.out.println("signup oke");

                    SharedPreferences sharedPref = getSharedPreferences("TmsLoginState", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt("user_id", result.getId());
                    editor.putString("username", result.getUsername());
                    editor.putBoolean("isLoggedIn", true);
                    editor.putString("fullname", result.getFullname());
                    editor.putString("email", result.getEmail());
                    editor.putString("team_name", result.getTeam().getTeam_name());
                    editor.putBoolean("isLoggedIn", true);
                    editor.apply();


                    editor.commit();

                    onSignupSuccess();

                } else if (result.getStatus()==1) {
                    _usernameText.setError("Username already exists");
                    isEmailFault = false;
                    onSignupFailed();
                    return;

                } else if (result.getStatus()==2) {
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


    public static User POST(String url, User person) {
        Gson gsonParser = new Gson();
        InputStream inputStream = null;
        User parsedResult = null;
        try {
            HttpHandler httpHandler = new HttpHandler();
            inputStream = httpHandler.requestAPI(url, person);
            // convert inputstream to Signup
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            parsedResult = gsonParser.fromJson(bufferedReader, User.class);
            inputStream.close();

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return parsedResult;
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

        if (isEmailFault){
            _emailText.requestFocus();
        }else {
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
}
