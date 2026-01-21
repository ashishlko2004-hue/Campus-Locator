package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private Button registerLink; // Changed from TextView
    private TextView forgotPasswordLink;
    private CheckBox rememberMeCheckbox;
    
    private MaterialCardView customAlertView;
    private Button customAlertOkButton;
    private TextView customAlertMessage;

    private FirebaseAuth mAuth;
    private SharedPreferences loginPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);

        if (mAuth.getCurrentUser() != null) {
            goToMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        registerLink = findViewById(R.id.register_link); 
        forgotPasswordLink = findViewById(R.id.forgot_password_link);
        rememberMeCheckbox = findViewById(R.id.remember_me_checkbox);
        
        customAlertView = findViewById(R.id.custom_alert_view);
        customAlertOkButton = findViewById(R.id.custom_alert_ok_button);
        customAlertMessage = findViewById(R.id.custom_alert_message);

        // Set styled text from strings.xml
        registerLink.setText(Html.fromHtml(getString(R.string.register_prompt_html), Html.FROM_HTML_MODE_LEGACY));

        String savedEmail = loginPreferences.getString("email", "");
        emailInput.setText(savedEmail);
        if(!savedEmail.isEmpty()){
            rememberMeCheckbox.setChecked(true);
        }

        loginButton.setOnClickListener(v -> handleLogin());
        registerLink.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        forgotPasswordLink.setOnClickListener(v -> handleForgotPassword());
        
        customAlertOkButton.setOnClickListener(v -> customAlertView.setVisibility(View.GONE));
    }

    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rememberMeCheckbox.isChecked()) {
            loginPreferences.edit().putString("email", email).apply();
        } else {
            loginPreferences.edit().remove("email").apply();
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    } else {
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleForgotPassword() {
        String email = emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        customAlertMessage.setText("A password reset link has been sent to " + email + ". Please check your inbox and spam folder.");
                        customAlertView.setVisibility(View.VISIBLE);
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "An unknown error occurred.";
                        Toast.makeText(LoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}