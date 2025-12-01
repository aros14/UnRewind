package com.example.unrewind;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    EditText username, password;
    Button loginButton;
    int attemptsRemaining = 3;

    final String correctUsername = "jsalazar";
    final String correctPassword = "12345";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        username = findViewById(R.id.etUsername);
        password = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCredentials();
            }
        });
    }

    private void checkCredentials() {
        String userInput = username.getText().toString().trim();
        String passInput = password.getText().toString().trim();

        if (userInput.equals(correctUsername) && passInput.equals(correctPassword)) {
            Toast.makeText(LoginActivity.this, "Redirecting...", Toast.LENGTH_LONG).show();

            // Go to MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close LoginActivity
        } else {
            attemptsRemaining--;
            if (attemptsRemaining > 0) {
                Toast.makeText(LoginActivity.this,
                        "Wrong Credentials! Attempts left: " + attemptsRemaining,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(LoginActivity.this,
                        "Too many failed attempts. Access blocked.",
                        Toast.LENGTH_LONG).show();
                loginButton.setEnabled(false); // disable login button
            }
        }
    }
}