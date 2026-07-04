package com.example.smsforwarder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Method;

public class XposedHook implements IXposedHookLoadPackage {
    
    private static final String TAG = "SmsForwarder";
    private static final String PREFS_NAME = "sms_forwarder_prefs";
    private static final String KEY_GATEWAY_URL = "gateway_url";
    private static final String KEY_API_TOKEN = "api_token";
    private static final String KEY_FORWARD_ENABLED = "forward_enabled";
    
    private static final String DEFAULT_GATEWAY_URL = "http://www.zhp98.fun:5005/api/sms/submit";
    private static final String DEFAULT_API_TOKEN = "Zhp199802!";
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.appInfo.processName.equals("com.android.providers.telephony")) {
            return;
        }
        
        XposedBridge.log(TAG + ": Hooking SMS provider in process: " + lpparam.appInfo.processName);
        
        // Hook the SMS receive method
        hookSmsReceive(lpparam);
    }
    
    private void hookSmsReceive(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook the dispatchBroadcast method in InboundSmsHandler
            String className = "com.android.internal.telephony.InboundSmsHandler";
            XposedHelpers.findAndHookMethod(
                className,
                lpparam.classLoader,
                "dispatchBroadcast",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Bundle bundle = (Bundle) param.args[0];
                            if (bundle == null) return;
                            
                            String format = bundle.getString("format");
                            if (!"3gpp".equals(format) && !"3gpp2".equals(format)) return;
                            
                            // Extract SMS data
                            byte[] pdu = bundle.getByteArray("pdu");
                            if (pdu == null) return;
                            
                            // Parse PDU to get sender and message
                            Object[] smsParts = parsePdu(pdu, format);
                            if (smsParts == null) return;
                            
                            String sender = (String) smsParts[0];
                            String message = (String) smsParts[1];
                            
                            // Get settings
                            Context context = (Context) XposedHelpers.callMethod(
                                XposedHelpers.callStaticMethod(
                                    Class.forName("android.app.ActivityThread"),
                                    "currentApplication"
                                ),
                                "getApplicationContext"
                            );
                            
                            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            boolean enabled = prefs.getBoolean(KEY_FORWARD_ENABLED, true);
                            
                            if (!enabled) return;
                            
                            String gatewayUrl = prefs.getString(KEY_GATEWAY_URL, DEFAULT_GATEWAY_URL);
                            String apiToken = prefs.getString(KEY_API_TOKEN, DEFAULT_API_TOKEN);
                            
                            // Forward SMS
                            forwardSms(gatewayUrl, apiToken, sender, message);
                            
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": Error processing SMS: " + t.getMessage());
                        }
                    }
                }
            );
            
            XposedBridge.log(TAG + ": Successfully hooked InboundSmsHandler");
            
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook SMS handler: " + t.getMessage());
            // Fallback: try alternative hook point
            hookAlternativeSmsReceive(lpparam);
        }
    }
    
    private void hookAlternativeSmsReceive(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Alternative: hook the SMS receive broadcast
            XposedHelpers.findAndHookMethod(
                "com.android.providers.telephony.SmsProvider",
                lpparam.classLoader,
                "insert",
                android.net.Uri.class,
                android.content.ContentValues.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            android.content.ContentValues values = (android.content.ContentValues) param.args[1];
                            if (values == null) return;
                            
                            String address = values.getAsString("address");
                            String body = values.getAsString("body");
                            
                            if (address == null || body == null) return;
                            
                            Context context = (Context) XposedHelpers.callMethod(
                                XposedHelpers.callStaticMethod(
                                    Class.forName("android.app.ActivityThread"),
                                    "currentApplication"
                                ),
                                "getApplicationContext"
                            );
                            
                            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            boolean enabled = prefs.getBoolean(KEY_FORWARD_ENABLED, true);
                            
                            if (!enabled) return;
                            
                            String gatewayUrl = prefs.getString(KEY_GATEWAY_URL, DEFAULT_GATEWAY_URL);
                            String apiToken = prefs.getString(KEY_API_TOKEN, DEFAULT_API_TOKEN);
                            
                            forwardSms(gatewayUrl, apiToken, address, body);
                            
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": Error in alternative hook: " + t.getMessage());
                        }
                    }
                }
            );
            
            XposedBridge.log(TAG + ": Hooked SmsProvider as fallback");
            
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook alternative SMS provider: " + t.getMessage());
        }
    }
    
    private Object[] parsePdu(byte[] pdu, String format) {
        try {
            // Simple PDU parser for text messages
            // This is a basic implementation - in production, use a proper SMS parser library
            
            int offset = 0;
            
            // SMSC length
            int smscLength = pdu[offset] & 0xFF;
            offset += 1 + smscLength;
            
            // First octet
            int firstOctet = pdu[offset] & 0xFF;
            offset += 1;
            
            // Sender length
            int senderLength = pdu[offset] & 0xFF;
            offset += 1;
            
            // Sender type
            int senderType = pdu[offset] & 0xFF;
            offset += 1;
            
            // Parse sender
            StringBuilder senderBuilder = new StringBuilder();
            int senderBytes = (senderLength + 1) / 2;
            for (int i = 0; i < senderBytes; i++) {
                int b = pdu[offset + i] & 0xFF;
                int low = b & 0x0F;
                int high = (b >> 4) & 0x0F;
                
                if (low != 0x0F) senderBuilder.append(low);
                if (high != 0x0F) senderBuilder.append(high);
            }
            offset += senderBytes;
            
            // PID and DCS
            offset += 2;
            
            // Timestamp
            offset += 7;
            
            // User data length
            int userDataLength = pdu[offset] & 0xFF;
            offset += 1;
            
            // Check for UDH
            boolean hasUdh = (firstOctet & 0x40) != 0;
            int udhLength = 0;
            if (hasUdh) {
                udhLength = pdu[offset] & 0xFF;
                offset += 1 + udhLength;
                userDataLength -= udhLength;
            }
            
            // Decode message
            int dcs = pdu[offset - userDataLength - udhLength - 1] & 0xFF;
            StringBuilder messageBuilder = new StringBuilder();
            
            if ((dcs & 0xF0) == 0x00) {
                // GSM 7-bit
                // ... (simplified implementation)
                messageBuilder.append("SMS content (GSM 7-bit)");
            } else if ((dcs & 0xF0) == 0x40) {
                // UCS2
                for (int i = 0; i < userDataLength; i += 2) {
                    int charCode = ((pdu[offset + i] & 0xFF) << 8) | (pdu[offset + i + 1] & 0xFF);
                    messageBuilder.append((char) charCode);
                }
            } else {
                // Default: try ASCII
                for (int i = 0; i < userDataLength; i++) {
                    messageBuilder.append((char) (pdu[offset + i] & 0xFF));
                }
            }
            
            return new Object[]{senderBuilder.toString(), messageBuilder.toString()};
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": PDU parsing error: " + e.getMessage());
            return null;
        }
    }
    
    private void forwardSms(String gatewayUrl, String token, String sender, String message) {
        new Thread(() -> {
            try {
                // Build JSON payload
                String json = String.format(
                    "{\"text\":\"%s\",\"sender\":\"%s\",\"timestamp\":\"%s\"}",
                    escapeJson(message),
                    escapeJson(sender),
                    java.time.Instant.now().toString()
                );
                
                // Make HTTP request
                java.net.URL url = new java.net.URL(gatewayUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                java.io.OutputStream os = conn.getOutputStream();
                os.write(json.getBytes("UTF-8"));
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                java.io.InputStream is = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
                
                java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                
                XposedBridge.log(TAG + ": Forwarded SMS to " + gatewayUrl + " - Response: " + responseCode);
                
                conn.disconnect();
                
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed to forward SMS: " + t.getMessage());
            }
        }).start();
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}