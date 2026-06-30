package com.integrafty.opexy.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// YOUTUBE WEBHOOK CONTROLLER
@Slf4j
@RestController
@RequestMapping("/api/youtube-webhook")
public class YouTubeWebhookController {

    // WEBHOOK VERIFICATION (GET)
    // Google will send a GET request here to verify you own this endpoint when you subscribe
    @GetMapping
    public ResponseEntity<String> verifySubscription(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String verifyToken) {
        
        log.info("[YOUTUBE WEBHOOK] Received verification request. Token: {}", verifyToken);
        
        // Return the challenge code to confirm subscription
        return new ResponseEntity<>(challenge, HttpStatus.OK);
    }

    // INCOMING NOTIFICATIONS (POST)
    // Google will send a POST request with XML data here when a stream starts or video uploads
    @PostMapping(consumes = "application/atom+xml")
    public ResponseEntity<String> receiveNotification(@RequestBody String xmlPayload) {
        log.info("[YOUTUBE WEBHOOK] 🚨 RECEIVED NEW NOTIFICATION 🚨");
        log.info("Payload: \n{}", xmlPayload);

        // Here you will parse the XML to get Video ID, Channel ID, and send to Discord
        // ...

        return new ResponseEntity<>("OK", HttpStatus.OK);
    }
}
