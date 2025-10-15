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
            var chatId = update.getMessage().getChatId().toString();
            var userMessage = update.getMessage().getText();
            String response;
            try {
                response = restTemplate.getForObject(
                        "http://localhost:8080/api/verse?query={query}",
                        String.class,
                        userMessage
                );
            } catch (Exception e) {
                response = "Sorry, I couldn’t find a suitable verse right now. Please try again later.";
            }

            assert response != null;
            SendMessage message = new SendMessage(chatId, response);
            message.enableMarkdown(true);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Exception during processing telegram api: {}", e.getMessage());
            }
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
}
