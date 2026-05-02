package com.agentic4j.core.memory;

import com.agentic4j.core.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SlidingWindowMemory implements ChatMemory {
    private final int maxMessages;
    private final LinkedList<ChatMessage> messages = new LinkedList<ChatMessage>();

    public SlidingWindowMemory(int maxMessages) {
        if (maxMessages < 1) {
            throw new IllegalArgumentException("maxMessages must be at least 1");
        }
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(ChatMessage message) {
        messages.addLast(message);
        while (messages.size() > maxMessages) {
            messages.removeFirst();
        }
    }

    @Override
    public List<ChatMessage> messages() {
        return Collections.unmodifiableList(new ArrayList<ChatMessage>(messages));
    }

    @Override
    public void clear() {
        messages.clear();
    }
}
