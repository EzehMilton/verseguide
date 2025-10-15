package com.chikere.verseguide.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
public class VerseGuideBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;

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
            var reply = "Thank you for your message: " + userMessage + " This is Chikere. Welcome to Verse Guide. The application is still in development";
            try {
                execute(new SendMessage(chatId, reply));
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
