package com.agentic4j.core;

import com.agentic4j.core.memory.ChatMemory;
import com.agentic4j.core.memory.SlidingWindowMemory;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class SlidingWindowMemoryTest {

    @Test
    public void testAddAndRetrieve() {
        ChatMemory memory = new SlidingWindowMemory(10);
        memory.add(ChatMessage.user("Hello"));
        memory.add(ChatMessage.assistant("Hi there"));
        List<ChatMessage> messages = memory.messages();
        assertEquals(2, messages.size());
        assertEquals("Hello", messages.get(0).getContent());
        assertEquals("Hi there", messages.get(1).getContent());
    }

    @Test
    public void testSlidingWindowEvictsOldest() {
        ChatMemory memory = new SlidingWindowMemory(3);
        memory.add(ChatMessage.user("msg1"));
        memory.add(ChatMessage.assistant("msg2"));
        memory.add(ChatMessage.user("msg3"));
        memory.add(ChatMessage.assistant("msg4"));
        List<ChatMessage> messages = memory.messages();
        assertEquals(3, messages.size());
        assertEquals("msg2", messages.get(0).getContent());
        assertEquals("msg3", messages.get(1).getContent());
        assertEquals("msg4", messages.get(2).getContent());
    }

    @Test
    public void testClear() {
        ChatMemory memory = new SlidingWindowMemory(10);
        memory.add(ChatMessage.user("Hello"));
        memory.clear();
        assertTrue(memory.messages().isEmpty());
    }

    @Test
    public void testWindowSizeOne() {
        ChatMemory memory = new SlidingWindowMemory(1);
        memory.add(ChatMessage.user("first"));
        memory.add(ChatMessage.user("second"));
        assertEquals(1, memory.messages().size());
        assertEquals("second", memory.messages().get(0).getContent());
    }

    @Test
    public void testMessagesReturnsCopy() {
        ChatMemory memory = new SlidingWindowMemory(10);
        memory.add(ChatMessage.user("Hello"));
        List<ChatMessage> messages = memory.messages();
        try {
            messages.add(ChatMessage.user("hack"));
            assertEquals(1, memory.messages().size());
        } catch (UnsupportedOperationException e) { }
    }
}
