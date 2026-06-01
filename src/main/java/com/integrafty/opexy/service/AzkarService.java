package com.integrafty.opexy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AzkarService {

    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    private List<ZikrItem> morningAzkar = new ArrayList<>();
    private List<ZikrItem> eveningAzkar = new ArrayList<>();
    private List<String> hourlyAzkar = new ArrayList<>();

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/azkar.json")) {
            if (is == null) {
                log.error("Could not find azkar.json in resources");
                return;
            }
            AzkarData data = objectMapper.readValue(is, AzkarData.class);
            if (data != null) {
                this.morningAzkar = data.getMorning() != null ? data.getMorning() : new ArrayList<>();
                this.eveningAzkar = data.getEvening() != null ? data.getEvening() : new ArrayList<>();
                this.hourlyAzkar = data.getHourly() != null ? data.getHourly() : new ArrayList<>();
                log.info("Successfully loaded Azkar: {} morning, {} evening, {} hourly",
                        morningAzkar.size(), eveningAzkar.size(), hourlyAzkar.size());
            }
        } catch (Exception e) {
            log.error("Failed to parse azkar.json", e);
        }
    }

    public List<ZikrItem> getMorningAzkar() {
        return Collections.unmodifiableList(morningAzkar);
    }

    public List<ZikrItem> getEveningAzkar() {
        return Collections.unmodifiableList(eveningAzkar);
    }

    public String getRandomHourlyZikr() {
        if (hourlyAzkar.isEmpty()) {
            return "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ.";
        }
        return hourlyAzkar.get(random.nextInt(hourlyAzkar.size()));
    }

    @Getter
    @Setter
    public static class ZikrItem {
        private String text;
        private String benefit;
        private int count;
    }

    @Getter
    @Setter
    public static class AzkarData {
        private List<ZikrItem> morning;
        private List<ZikrItem> evening;
        private List<String> hourly;
    }
}
