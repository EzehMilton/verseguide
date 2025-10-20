package com.chikere.verseguide.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Telegram bot that provides Bible verse searches with daily rate limiting.
 * Allows users to search for verses by keywords and provides reflections.
 */
@Component
@Slf4j
public class VerseGuideBot extends TelegramLongPollingBot {

    private static final int MAX_QUERY_LENGTH = 200;
    private static final int API_TIMEOUT_SECONDS = 10;

    private final String botUsername;
    private final String botToken;
    private final int dailyLimit;
    private final String verseApiUrl;
    private final WebClient webClient;

    private final Map<Long, UserUsage> userUsageMap = new ConcurrentHashMap<>();

    public VerseGuideBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.daily-limit:5}") int dailyLimit,
            @Value("${verse.api.url:http://localhost:8080/api/verse}") String verseApiUrl,
            WebClient.Builder webClientBuilder
    ) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.dailyLimit = dailyLimit;
        this.verseApiUrl = verseApiUrl;
        this.webClient = webClientBuilder
                .baseUrl(verseApiUrl)
                .build();

        log.info("VerseGuideBot initialized - Application Name: {}, Daily Limit: {}, Application Url: {}",
                botUsername, dailyLimit, verseApiUrl);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || update.getMessage() == null || !update.getMessage().hasText()) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        String userText = update.getMessage().getText().trim();

        try {
            handleUserMessage(chatId, userText);
        } catch (Exception e) {
            log.error("Error processing message from user {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "❌ An unexpected error occurred. Please try again later. Thank you!");
        }
    }

    /**
     * Routes user messages to appropriate handlers based on command or query
     */
    private void handleUserMessage(Long chatId, String userText) {
        switch (userText.toLowerCase()) {
            case "/start" -> handleStartCommand(chatId);
            case "/help" -> handleHelpCommand(chatId);
            case "/status" -> handleStatusCommand(chatId);
            case "/reset" -> handleResetCommand(chatId);
            default -> handleVerseQuery(chatId, userText);
        }
    }

    /**
     * Handles the /start command
     */
    private void handleStartCommand(Long chatId) {
        String welcomeMessage = String.format("""
                🌿 *Welcome to VerseGuide!*
                Explore Bible verses and reflections that offer clarity, peace, and inspiration for whatever you’re facing today.
                
                📖 *How to use:*
                Type a word or phrase like *"Hope"*, *"Lord, give me strength"*, or *"God, I need you right now"*.
                VerseGuide will share a matching Bible verse and reflection 🙏.
                
                📊 *Commands:*
                /help - Show this help message
                /status - Check your remaining requests
                
                ⚖️ *Rate Limit:* %d requests per day. (Free plan)
                
                — VerseGuide by Chikere Ezeh
                """, dailyLimit);
        sendMessage(chatId, welcomeMessage);
    }

    /**
     * Handles the /help command
     */
    private void handleHelpCommand(Long chatId) {
        handleStartCommand(chatId);
    }

    /**
     * Handles the /status command
     */
    private void handleStatusCommand(Long chatId) {
        int remaining = getRemainingRequests(chatId);
        int used = dailyLimit - remaining;

        String statusMessage = String.format("""
                📊 *Your Daily Status (Free plan)*
                
                ✅ Used: %d request(s)
                🔄 Remaining: %d request(s)
                📅 Limit: %d per day
                🕐 Resets: Midnight (your time)
                """, used, remaining, dailyLimit);

        sendMessage(chatId, statusMessage);
    }

    /**
     * Handles the /reset command (for testing purposes)
     */
    private void handleResetCommand(Long chatId) {
        userUsageMap.remove(chatId);
        log.info("User {} manually reset their daily limit", chatId);
        sendMessage(chatId, "✅ Your daily limit has been reset successfully.");
    }

    /**
     * Handles verse search queries
     */
    private void handleVerseQuery(Long chatId, String query) {
        if (query.isBlank()) {
            sendMessage(chatId, "⚠️ Please enter a word or phrase to search for Bible verses.");
            return;
        }

        if (query.length() > MAX_QUERY_LENGTH) {
            sendMessage(chatId, String.format(
                    "⚠️ Your query is too long. Please keep it under %d characters.",
                    MAX_QUERY_LENGTH));
            return;
        }

        if (!canMakeRequest(chatId)) {
            sendMessage(chatId, String.format("""
                    ⚠️ You've reached your daily limit of %d requests.
                    Your free plan resets at midnight.. 🙏
                    
                    Use /status to check your remaining requests.
                    """, dailyLimit));
            return;
        }

        recordUsage(chatId);
        String response = fetchVerseFromApi(query);
        int remaining = getRemainingRequests(chatId);
        response += String.format("\n\n📊 Requests left today: *%d/%d*", remaining, dailyLimit);
        sendMessage(chatId, response);
    }

    /**
     * Fetches verse data from the backend API
     */
    private String fetchVerseFromApi(String query) {
        try {
            log.debug("Fetching verse for query: {}", query);

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(API_TIMEOUT_SECONDS))
                    .block();

            if (response == null || response.isBlank()) {
                return "📖 No Bible verse found for that phrase. Try another keyword like *\"faith\"*, *\"love\"*, or *\"strength\"*.";
            }

            return response;

        } catch (Exception e) {
            log.error("Error calling verse API for query '{}': {}", query, e.getMessage());
            return "❌ Sorry, something went wrong while searching for Bible verses. Please try again later.";
        }
    }

    /**
     * Checks if user can make a request without recording usage
     */
    private boolean canMakeRequest(Long chatId) {
        return getRemainingRequests(chatId) > 0;
    }

    /**
     * Gets remaining requests for the user today
     */
    private int getRemainingRequests(Long chatId) {
        LocalDate today = LocalDate.now();
        UserUsage usage = userUsageMap.get(chatId);

        if (usage == null || !usage.date.equals(today)) {
            return dailyLimit;
        }

        return Math.max(0, dailyLimit - usage.count.get());
    }

    /**
     * Records a request for the user
     */
    private synchronized void recordUsage(Long chatId) {
        LocalDate today = LocalDate.now();
        UserUsage usage = userUsageMap.get(chatId);

        if (usage == null || !usage.date.equals(today)) {
            // New day or new user
            userUsageMap.put(chatId, new UserUsage(today));
            log.info("New usage record created for user {}", chatId);
        } else {
            usage.count.incrementAndGet();
        }

        int currentCount = userUsageMap.get(chatId).count.get();
        int remaining = dailyLimit - currentCount;
        log.info("User {} - Used: {}, Remaining: {}", chatId, currentCount, remaining);
    }

    /**
     * Sends a message to the user
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        msg.enableMarkdown(true);

        try {
            execute(msg);
            log.debug("Message sent to user {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to user {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Scheduled task to clean up old usage data (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldUsageData() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int removedCount = 0;

        var iterator = userUsageMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().date.isBefore(yesterday)) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} old usage records", removedCount);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Internal class to track user usage per day
     */
    private static class UserUsage {
        final LocalDate date;
        final AtomicInteger count;

        UserUsage(LocalDate date) {
            this.date = date;
            this.count = new AtomicInteger(1);
        }
    }
}