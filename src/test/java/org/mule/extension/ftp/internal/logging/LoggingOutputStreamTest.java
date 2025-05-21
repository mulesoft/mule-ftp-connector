package org.mule.extension.ftp.internal.logging;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class LoggingOutputStreamTest {

    private LoggingOutputStream loggingOutputStream;
    private List<String> capturedOutput;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        capturedOutput = new ArrayList<>();
        loggingOutputStream = new LoggingOutputStream(capturedOutput::add);
    }

    @Test
    public void testWriteSingleCharacter() throws IOException {
        loggingOutputStream.write('a');
        assertEquals(0, capturedOutput.size());
    }

    @Test
    public void testWriteCompleteLine() throws IOException {
        String testString = "Hello World\n";
        for (byte b : testString.getBytes()) {
            loggingOutputStream.write(b);
        }
        assertEquals(1, capturedOutput.size());
        assertEquals("Hello World", capturedOutput.get(0));
    }

    @Test
    public void testWriteMultipleLines() throws IOException {
        String testString = "Line 1\nLine 2\nLine 3\n";
        for (byte b : testString.getBytes()) {
            loggingOutputStream.write(b);
        }
        assertEquals(3, capturedOutput.size());
        assertEquals("Line 1", capturedOutput.get(0));
        assertEquals("Line 2", capturedOutput.get(1));
        assertEquals("Line 3", capturedOutput.get(2));
    }

    @Test
    public void testWriteWithoutNewline() throws IOException {
        String testString = "No newline here";
        for (byte b : testString.getBytes()) {
            loggingOutputStream.write(b);
        }
        assertEquals(0, capturedOutput.size());
    }

    @Test
    public void testWriteWithMockConsumer() throws IOException {
        Consumer<String> mockConsumer = mock(Consumer.class);
        LoggingOutputStream stream = new LoggingOutputStream(mockConsumer);
        
        String testString = "Test line\n";
        for (byte b : testString.getBytes()) {
            stream.write(b);
        }
        
        verify(mockConsumer, times(1)).accept("Test line");
    }

    @Test
    public void testWriteWithSpecialCharacters() throws IOException {
        String testString = "Special chars: !@#$%\n";
        for (byte b : testString.getBytes()) {
            loggingOutputStream.write(b);
        }
        assertEquals(1, capturedOutput.size());
        assertEquals("Special chars: !@#$%", capturedOutput.get(0));
    }
} 