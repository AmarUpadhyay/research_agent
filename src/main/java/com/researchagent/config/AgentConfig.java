package com.researchagent.config;

import com.researchagent.agent.TaskAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    public TaskAgent taskAgent(ChatLanguageModel model) {
        return AiServices.builder(TaskAgent.class)
                .chatLanguageModel(model)
                .build();
    }
}
