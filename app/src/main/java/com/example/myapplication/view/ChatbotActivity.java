package com.example.myapplication.view;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.ChatAdapter;
import com.example.myapplication.model.ChatMessage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatbotActivity extends AppCompatActivity {

    // TODO: Add your Groq API key to local.properties: GROQ_API_KEY=your_key_here
    private static final String GROQ_API_KEY = "YOUR_GROQ_API_KEY_HERE"; // Replace with your actual key
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    
    private RecyclerView rvChatMessages;
    private EditText etMessage;
    private FloatingActionButton fabSend;
    
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize views
        rvChatMessages = findViewById(R.id.rv_chat_messages);
        etMessage = findViewById(R.id.et_message);
        fabSend = findViewById(R.id.fab_send);

        // Initialize data
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        client = new OkHttpClient();

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(chatAdapter);

        // Welcome message
        addMessage(new ChatMessage(
            "Hello! I'm your AI Fitness Assistant powered by Groq. How can I help you today?",
            "Groq AI",
            getCurrentTime(),
            false
        ));

        // Send button click
        fabSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add user message
        addMessage(new ChatMessage(messageText, "You", getCurrentTime(), true));
        etMessage.setText("");

        // Show typing indicator
        Toast.makeText(this, "AI is thinking...", Toast.LENGTH_SHORT).show();

        // Call Grok API
        callGrokAPI(messageText);
    }

    private void callGrokAPI(String userMessage) {
        try {
            // Create proper JSON using JSONObject
            org.json.JSONObject jsonRequest = new org.json.JSONObject();
            org.json.JSONArray messages = new org.json.JSONArray();
            
            // System message
            org.json.JSONObject systemMsg = new org.json.JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a helpful AI fitness assistant. Provide helpful, motivational, and accurate fitness advice.");
            messages.put(systemMsg);
            
            // User message
            org.json.JSONObject userMsg = new org.json.JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.put(userMsg);
            
            jsonRequest.put("messages", messages);
            jsonRequest.put("model", "llama-3.3-70b-versatile");
            jsonRequest.put("stream", false);
            jsonRequest.put("temperature", 0);

            RequestBody body = RequestBody.create(
                jsonRequest.toString(),
                MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(GROQ_API_URL)
                    .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        addMessage(new ChatMessage(
                            "Sorry, I'm having trouble connecting. Please check your internet connection and try again.\n\nError: " + e.getMessage(),
                            "Groq AI",
                            getCurrentTime(),
                            false
                        ));
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    if (response.isSuccessful()) {
                        try {
                            org.json.JSONObject jsonObject = new org.json.JSONObject(responseBody);
                            String aiResponse = jsonObject
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                            runOnUiThread(() -> {
                                addMessage(new ChatMessage(
                                    aiResponse.trim(),
                                    "Groq AI",
                                    getCurrentTime(),
                                    false
                                ));
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                addMessage(new ChatMessage(
                                    "Sorry, I couldn't process the response.\n\nResponse: " + responseBody,
                                    "Groq AI",
                                    getCurrentTime(),
                                    false
                                ));
                            });
                        }
                    } else {
                        // Log error details
                        String errorMsg = "Error " + response.code() + ": " + response.message() + 
                                        "\n\nDetails: " + responseBody;
                        runOnUiThread(() -> {
                            addMessage(new ChatMessage(
                                "Sorry, the server returned an error.\n\n" + errorMsg,
                                "Groq AI",
                                getCurrentTime(),
                                false
                            ));
                        });
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addMessage(ChatMessage message) {
        messages.add(message);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        rvChatMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
    }
}
