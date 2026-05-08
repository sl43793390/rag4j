package com.sl.rag4j.model;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface RagAgent {


    @Agent()
//    @Agent(outputKey = "story", description = "Generates a story based on the given topic")
    String chat(@MemoryId String memoryId, @V("topic") String topic);
}
