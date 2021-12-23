package net.justchunks.openmatch.client.observer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallbackStreamObserverTest {

    @Test
    @DisplayName("Should log waring during transmission")
    @Disabled("The verification always fails within PITest (but works normally)")
    void shouldLogWarning() {
        // given
        Appender appender = mock(Appender.class);
        when(appender.getName()).thenReturn("TestAppender");
        when(appender.isStarted()).thenReturn(true);
        Logger callbackLogger = (Logger) LogManager.getLogger(CallbackStreamObserver.class);
        callbackLogger.addAppender(appender);
        callbackLogger.setLevel(Level.ALL);
        CallbackStreamObserver<Void> observer = CallbackStreamObserver.getInstance(ignored -> {
            // intentionally empty
        });
        Throwable throwable = new IllegalStateException();

        // when
        observer.onError(throwable);

        // then
        verify(appender, times(1)).append(any());
    }

    @Test
    @DisplayName("Should get a valid singleton instance")
    void shouldGetInstance() {
        // given
        CallbackStreamObserver<Void> observer = CallbackStreamObserver.getInstance(ignored -> {
            // intentionally empty
        });

        // then
        Assertions.assertNotNull(observer);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should trigger the callback for next element")
    void shouldTriggerCallback() {
        // given
        Consumer<String> consumer = (Consumer<String>) mock(Consumer.class);
        CallbackStreamObserver<String> observer = CallbackStreamObserver.getInstance(consumer);

        // when
        observer.onNext("test");

        // then
        verify(consumer, Mockito.times(1)).accept("test");
    }
}
