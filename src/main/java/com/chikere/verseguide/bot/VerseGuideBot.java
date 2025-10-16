package com.chikere.verseguide.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class VerseGuideBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final RestTemplate restTemplate = new RestTemplate();

    public VerseGuideBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken
    ) {
        this.botUsername = botUsername;
        this.botToken = botToken;

        log.debug(">>> Loaded from properties: username={} | token startsWith={}",
                botUsername, botToken.substring(0, 6));
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String userText = update.getMessage().getText().trim();

            // Welcome on /start
            if (userText.equalsIgnoreCase("/start")) {
                SendMessage welcomeMsg = getWelcomeMsg(chatId);
                try {
                    execute(welcomeMsg);
                } catch (TelegramApiException e) {
                    log.error("Failed to send welcome message: {}", e.getMessage());
                }
                return;
            }

            // Otherwise treat it as a verse request
            String response;
            try {
                response = restTemplate.getForObject(
                        "http://localhost:8080/api/verse?query={query}",
                        String.class,
                        userText
                );
            } catch (Exception e) {
                log.error("Error calling verse API: {}", e.getMessage());
                response = "Sorry, I couldn’t find a suitable verse right now. Please try again later.";
            }
            if (response == null) {
                response = "No verse found.";
            }

            SendMessage message = new SendMessage(chatId, response);
            message.enableMarkdown(true);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Exception sending verse response: {}", e.getMessage());
            }
        }
    }

    private static SendMessage getWelcomeMsg(String chatId) {
        String welcome = """
                 🌿 Welcome to VerseGuide! \s
                 Find Bible verses and reflections that bring clarity, peace, or inspiration at times of need. \s
                 Just type a word or phrase like *“hope”*, *“forgiveness”*, or *“ seeking peace”*  \s
                 and VerseGuide will provide a verse and short reflection that match your theme. \s
                 Start whenever you’re ready.
                 Thanks Chikere Ezeh 🙏.
               """;
        SendMessage welcomeMsg = new SendMessage(chatId, welcome);
        welcomeMsg.enableMarkdown(true);
        return welcomeMsg;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
