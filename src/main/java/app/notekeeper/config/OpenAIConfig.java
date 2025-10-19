package app.notekeeper.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OpenAIConfig {

    @Bean
    @Primary
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @Qualifier("powerfulChatClient")
    public ChatClient powerfulChatClient(ChatClient.Builder builder,
            @Value("${app.openai.powerful-model.name}") String powerfulModelName) {
        return builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(powerfulModelName)
                        .temperature(1.0)
                        .build())
                .build();
    }

}
