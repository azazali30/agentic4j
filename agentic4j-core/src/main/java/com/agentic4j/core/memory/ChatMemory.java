package com.agentic4j.core.memory;

import com.agentic4j.core.ChatMessage;
import java.util.List;

public interface ChatMemory {
    void add(ChatMessage message);
    List<ChatMessage> messages();
    void clear();
}
