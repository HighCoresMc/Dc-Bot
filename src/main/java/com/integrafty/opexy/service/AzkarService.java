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
    private List<ZikrItem> hourlyAzkarItems = new ArrayList<>();

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
                
                this.hourlyAzkarItems = new ArrayList<>();
                for (String text : this.hourlyAzkar) {
                    ZikrItem item = new ZikrItem();
                    item.setText(text);
                    item.setBenefit(getBenefitForText(text));
                    item.setCount(1);
                    this.hourlyAzkarItems.add(item);
                }
                log.info("Successfully loaded Azkar: {} morning, {} evening, {} hourly items",
                        morningAzkar.size(), eveningAzkar.size(), hourlyAzkarItems.size());
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

    public ZikrItem getRandomHourlyZikrItem() {
        if (hourlyAzkarItems.isEmpty()) {
            ZikrItem fallback = new ZikrItem();
            fallback.setText("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ.");
            fallback.setBenefit("كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ.");
            fallback.setCount(1);
            return fallback;
        }
        return hourlyAzkarItems.get(random.nextInt(hourlyAzkarItems.size()));
    }

    private String getBenefitForText(String text) {
        if (text == null) return "";
        String clean = text.replace(".", "").trim();
        if (clean.contains("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ") && clean.contains("سُبْحَانَ اللَّهِ الْعَظِيمِ")) {
            return "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ.";
        }
        if (clean.equals("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ") || clean.equals("سُبْحَانَ اللهِ وَبِحَمْدِهِ")) {
            return "حُطَّتْ خَطَايَاهُ وَإِنْ كَانَتْ مِثْلَ زَبَدِ الْبَحْرِ.";
        }
        if (clean.equals("سُبْحَانَ اللَّهِ الْعَظِيمِ") || clean.equals("سُبْحَانَ اللهِ الْعَظِيمِ")) {
            return "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ.";
        }
        if (clean.contains("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ") || clean.contains("لا حَوْلَ وَلا قُوَّةَ إِلاَّ بِاللَّهِ") || clean.contains("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ")) {
            return "كَنْزٌ مِنْ كُنُوزِ الْجَنَّةِ.";
        }
        if (clean.contains("أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ") || clean.contains("أَسْتَغْفِرُ اللهَ الْعَظِيمَ") || clean.contains("أسْتَغْفِرُ اللهَ العَظِيمَ")) {
            return "مَنْ لَزِمَ الِاسْتِغْفَارَ جَعَلَ اللَّهُ لَهُ مِنْ كُلِّ ضِيقٍ مَخْرَجًا، وَمِنْ كُلِّ هَمٍّ فَرَجًا.";
        }
        if (clean.contains("لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ") || clean.contains("لَا إلَه إلّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ") || clean.contains("لا إلهَ إلاّ اللّهُ وَحدَهُ لا شَريكَ لهُ")) {
            return "كَانَتْ لَهُ عَدْلَ عَشْرِ رِقَابٍ، وَكُتِبَتْ لَهُ مِائَةُ حَسَنَةٍ، وَمُحِيَتْ عَنْهُ مِائَةُ سَيِّئَةٍ.";
        }
        if (clean.contains("اللَّهُمَّ صَلِّ وَسَلِّمْ") || clean.contains("اللَّهُمَّ صَلِّ وَسَلِّمْ") || clean.contains("اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ")) {
            return "مَنْ صَلَّى عَلَيَّ صَلَاةً صَلَّى اللَّهُ عَلَيْهِ بِهَا عَشْرًا.";
        }
        if (clean.contains("سُبْحَانَ اللَّهِ") && clean.contains("الْحَمْدُ لِلَّهِ") && clean.contains("لَا إِلَهَ إِلَّا اللَّهُ")) {
            return "أَحَبُّ الْكَلَامِ إِلَى اللَّهِ.";
        }
        if (clean.contains("لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ") || clean.contains("لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إنِّي كُنْتُ مِنَ الظَّالِمِينَ")) {
            return "لَمْ يَدْعُ بِهَا رَجُلٌ مُسْلِمٌ فِي شَيْءٍ قَطُّ إِلَّا اسْتَجَابَ اللَّهُ لَهُ.";
        }
        if (clean.contains("أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ") || clean.contains("أَسْتَغْفِرُ اللهَ وَأَتُوبُ إِلَيْهِ") || clean.contains("أسْتَغْفِرُ اللهَ وَأتُوبُ إلَيْهِ") || clean.contains("أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إلَيْهِ")) {
            return "مائة حسنة، ومُحيت عنه مائة سيئة، وكانت له حرزاً من الشيطان حتى يمسى.";
        }
        if (clean.contains("سُبْحَانَ اللَّهِ الْعَظِيمِ وَبِحَمْدِهِ") || clean.contains("سُبْحَانَ اللهِ الْعَظِيمِ وَبِحَمْدِهِ")) {
            return "غُرِسَتْ لَهُ نَخْلَةٌ فِي الْجَنَّةِ.";
        }
        return "";
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
