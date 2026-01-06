# Groq AI Chatbot Setup

## API Key Configuration

**IMPORTANT:** For security, the Groq API key is NOT included in this repository.

### Setup Instructions:

1. Get your Groq API key from: https://console.groq.com/keys

2. Open `ChatbotActivity.java` and replace the placeholder:
   ```java
   private static final String GROQ_API_KEY = "YOUR_ACTUAL_KEY_HERE";
   ```

3. **Alternative (Recommended):** Use BuildConfig:
   - Add to `local.properties`:
     ```
     GROQ_API_KEY=your_key_here
     ```
   - Update `build.gradle.kts` to read from properties
   - Use `BuildConfig.GROQ_API_KEY` in code

### Current API Key Location:
- File: `app/src/main/java/com/example/myapplication/view/ChatbotActivity.java`
- Line: 34

**Never commit API keys to Git!**
