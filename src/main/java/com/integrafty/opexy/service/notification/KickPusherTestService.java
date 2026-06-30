package com.integrafty.opexy.service.notification;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionStateChange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

// KICK PUSHER TEST SERVICE
@Slf4j
@Service
public class KickPusherTestService {

    // PUSHER CONFIGURATION
    private static final String KICK_PUSHER_KEY = "32cbd69e4b950bf97679";
    private static final String KICK_CLUSTER = "us2";
    
    // TARGET CHANNEL ID (1184 = xQc)
    private static final String TEST_CHANNEL_ID = "1184";

    // STANDALONE TEST MAIN METHOD
    public static void main(String[] args) {
        KickPusherTestService service = new KickPusherTestService();
        service.initPusher();
        
        try {
            Thread.sleep(100000); // Keep alive
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // INITIALIZATION
    @PostConstruct
    public void initPusher() {
        PusherOptions options = new PusherOptions();
        options.setCluster(KICK_CLUSTER);
        Pusher pusher = new Pusher(KICK_PUSHER_KEY, options);

        // CONNECTION EVENTS
        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                log.info("[KICK PUSHER] State: {}", change.getCurrentState());
            }

            @Override
            public void onError(String message, String code, Exception e) {
                log.error("[KICK PUSHER] Error: {}", message);
            }
        });

        // CHANNEL SUBSCRIPTION
        Channel channel = pusher.subscribe("channel." + TEST_CHANNEL_ID);
        log.info("[KICK PUSHER] Listening to channel ID: {}", TEST_CHANNEL_ID);

        // EVENT LISTENERS
        channel.bind("App\\Events\\StreamerIsLive", new SubscriptionEventListener() {
            @Override
            public void onEvent(PusherEvent event) {
                log.info("[KICK PUSHER] STREAMER IS LIVE: {}", event.getData());
            }
        });

        channel.bind("App\\Events\\StreamerIsOffline", new SubscriptionEventListener() {
            @Override
            public void onEvent(PusherEvent event) {
                log.info("[KICK PUSHER] STREAMER WENT OFFLINE.");
            }
        });
    }
}
