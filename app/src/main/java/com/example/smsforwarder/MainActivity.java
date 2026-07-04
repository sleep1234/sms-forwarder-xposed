package com.example.smsforwarder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "sms_forwarder_prefs";
    private static final String KEY_GATEWAY_URL = "gateway_url";
    private static final String KEY_API_TOKEN = "api_token";
    private static final String KEY_FORWARD_ENABLED = "forward_enabled";
    
    private EditText etGatewayUrl, etApiToken;
    private Switch switchEnabled;
    private Button btnSave, btnTest;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize views
        etGatewayUrl = findViewById(R.id.et_gateway_url);
        etApiToken = findViewById(R.id.et_api_token);
        switchEnabled = findViewById(R.id.switch_enabled);
        btnSave = findViewById(R.id.btn_save);
        btnTest = findViewById(R.id.btn_test);
        
        // Load saved settings
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        etGatewayUrl.setText(prefs.getString(KEY_GATEWAY_URL, "http://www.zhp98.fun:5005/api/sms/submit"));
        etApiToken.setText(prefs.getString(KEY_API_TOKEN, "Zhp199802!"));
        switchEnabled.setChecked(prefs.getBoolean(KEY_FORWARD_ENABLED, true));
        
        // Save button click listener
        btnSave.setOnClickListener(v -> saveSettings());
        
        // Test button click listener
        btnTest.setOnClickListener(v -> testConnection());
        
        // Check if module is active
        checkModuleStatus();
    }
    
    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_GATEWAY_URL, etGatewayUrl.getText().toString().trim());
        editor.putString(KEY_API_TOKEN, etApiToken.getText().toString().trim());
        editor.putBoolean(KEY_FORWARD_ENABLED, switchEnabled.isChecked());
        editor.apply();
        
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
    
    private void testConnection() {
        String url = etGatewayUrl.getText().toString().trim();
        String token = etApiToken.getText().toString().trim();
        
        if (url.isEmpty()) {
            Toast.makeText(this, "请输入网关地址", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            try {
                // Build test payload
                String json = "{\"text\":\"Test SMS from LSPosed module\",\"sender\":\"1234567890\",\"timestamp\":\"" + 
                    java.time.Instant.now().toString() + "\"}";
                
                URL testUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) testUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes("UTF-8"));
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                BufferedReader reader;
                if (responseCode < 400) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                conn.disconnect();
                
                runOnUiThread(() -> {
                    if (responseCode >= 200 && responseCode < 300) {
                        Toast.makeText(this, R.string.connection_success + "\n" + response.toString(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.connection_failed + "HTTP " + responseCode + "\n" + response.toString(), Toast.LENGTH_LONG).show();
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.connection_failed + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void checkModuleStatus() {
        Toast.makeText(this, R.string.module_not_active + "\n请在 LSPosed 管理器中激活", Toast.LENGTH_LONG).show();
    }
}