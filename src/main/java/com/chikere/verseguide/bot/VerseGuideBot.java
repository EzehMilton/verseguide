package com.chikere.verseguide.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class VerseGuideBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final int dailyLimit;
    private final RestTemplate restTemplate = new RestTemplate();

    // Store user usage data per day
    private final Map<Long, UserUsage> userUsageMap = new ConcurrentHashMap<>();

    public VerseGuideBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.daily-limit:5}") int dailyLimit // default to 5 if not set
    ) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.dailyLimit = dailyLimit;

        log.debug(">>> Loaded from properties: username={} | token prefix={} | dailyLimit={}",
                botUsername,
                botToken != null && botToken.length() >= 6 ? botToken.substring(0, 6) : "null",
                dailyLimit);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Long chatId = update.getMessage().getChatId();
        String userText = update.getMessage().getText().trim();

        // 🌿 1. Handle /start
        if (userText.equalsIgnoreCase("/start")) {
            sendMessage(chatId, getWelcomeMessage());
            return;
        }

        if (userText.equalsIgnoreCase("/reset")) {
            userUsageMap.remove(chatId);
            sendMessage(chatId, "✅ Your daily limit has been reset.");
            return;
        }

        // 🧮 2. Check if user can still use bot
        int remaining = getRemainingRequests(chatId);
        if (remaining <= 0) {
            sendMessage(chatId, String.format("""
                    ⚠️ You’ve reached your daily limit of %d requests.
                    Please come back tomorrow. 🙏
                    """, dailyLimit));
            return;
        }

        // 📖 3. Get verse from backend API
        String response;
        try {
            response = restTemplate.getForObject(
                    "http://localhost:8080/api/verse?query={query}",
                    String.class,
                    userText
            );
        } catch (Exception e) {
            log.error("Error calling verse API: {}", e.getMessage());
            response = "Sorry, something went wrong while finding a verse. Please try again later.";
        }

        if (response == null || response.isBlank()) {
            response = "No verse found for that phrase. Try another keyword.";
        }

        // 📅 Add usage info to message
        response += String.format("\n\n📅 You have %d of %d requests left today.", remaining - 1, dailyLimit);

        sendMessage(chatId, response);
    }

    /**
     * Checks and updates daily usage for the given user.
     * Returns the number of requests remaining for today.
     */
    private int getRemainingRequests(Long chatId) {
        LocalDate today = LocalDate.now();
        UserUsage usage = userUsageMap.get(chatId);

        if (usage == null || !usage.date.equals(today)) {
            userUsageMap.put(chatId, new UserUsage(today, 1));
            log.info("🆕 New user/day {} -> count = 1", chatId);
            return dailyLimit - 1;
        }

        if (usage.count >= dailyLimit) {
            log.info("🚫 User {} exceeded limit ({} requests)", chatId, usage.count);
            return 0;
        }

        usage.count++;
        int remaining = dailyLimit - usage.count;
        log.info("📈 User {} -> used: {}, remaining: {}", chatId, usage.count, remaining);
        return remaining + 1; // Return remaining *before* next request
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        msg.enableMarkdown(true);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Telegram error: {}", e.getMessage());
        }
    }

    private String getWelcomeMessage() {
        return String.format("""
                🌿 *Welcome to VerseGuide!*  
                Discover Bible verses and reflections that bring clarity, peace, or inspiration.  
                Type a word or phrase like *“hope”*, *“forgiveness”*, or *“peace”*.  
                VerseGuide will share a matching verse and reflection.  
                
                Each user can make up to %d requests per day.  
                — VerseGuide by Chikere Ezeh 🙏
                """, dailyLimit);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private static class UserUsage {
        LocalDate date;
        int count;

        UserUsage(LocalDate date, int count) {
            this.date = date;
            this.count = count;
        }
    }
}