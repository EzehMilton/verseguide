package com.chikere.verseguide.controller;

import com.chikere.verseguide.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VerseController {

    private final ChatClient chatClient;
    private final PromptLoader promptLoader;

    @GetMapping("/verse")
    public String getVerse(@RequestParam String query) throws Exception {
        String promptTemplate = promptLoader.loadPrompt("verse_prompt.txt");
        String promptText = String.format(promptTemplate, query);
        Prompt prompt = new Prompt(new UserMessage(promptText));
        return chatClient.prompt(prompt)
                .call()
                .content();
    }


}
