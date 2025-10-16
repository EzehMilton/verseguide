package com.chikere.verseguide.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class PromptLoader {
    public String loadPrompt(String filename) {
        try {
            var resource = new ClassPathResource("prompts/" + filename);
            Path path = resource.getFile().toPath();
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt: " + filename, e);
        }
    }
}
