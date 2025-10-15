package com.chikere.verseguide.controller;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VerseController {

    @Autowired
    private OpenAiChatModel model;

    @GetMapping("/verse")
    public String getVerse(@RequestParam String query) {

        String promptText = """
                You are a compassionate Christian spiritual assistant designed to support people through emotional or spiritual struggles using the Bible.
                A person says: "%s".
                Your task:
                1. Gently identify their underlying emotional or spiritual need. Speak directly to them with empathy, not in the third person.
                2. Provide ONE relevant Bible verse that addresses their need. Quote the verse along with the book, chapter, and verse number.
                3. PWrite a short, encouraging reflection (2–3 sentences) that explains why this verse is meaningful in light of their situation. Use warm, pastoral language and avoid sounding generic or robotic.
                Use this format exactly::
                📖 Verse: <quoted scripture with reference>
                🕊️ Reflection: <personal, uplifting explanation>
                """.formatted(query);

        Prompt prompt = new Prompt(new UserMessage(promptText));
        return model.call(prompt)
                .getResult()
                .getOutput()
                .getText();
    }


}
