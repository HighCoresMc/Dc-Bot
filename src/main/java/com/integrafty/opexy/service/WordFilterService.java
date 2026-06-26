package com.integrafty.opexy.service;

import com.integrafty.opexy.entity.WordFilterEntity;
import com.integrafty.opexy.repository.WordFilterRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordFilterService {

    private final WordFilterRepository wordFilterRepository;

    private final Set<String> forbiddenWords = new HashSet<>();

    private final List<CompiledFilter> strictFilters = new ArrayList<>();
    private final List<CompiledFilter> contextFilters = new ArrayList<>();

    private static final Set<String> STRICT_ROOTS = Set.of(
        "http:", "https:", "184.57.51.245", "1190305586710073427",
        "زبوري", "نيك", "تناك", "منوك", "ينيك", "قحبة", "قحبه", "قاحيب", 
        "شرموطه", "شرموطة", "شراميط", "عرص", "ديوث", "شاذ", "زب", "خنيث", 
        "اير", "كس", "طيز", "خرا", "ابن الحرام", "عيال الحرام", "ابن ال", "طيرك", "طيري"
    );

    private static class CompiledFilter {
        private final String originalWord;
        private final Pattern pattern;

        public CompiledFilter(String originalWord, Pattern pattern) {
            this.originalWord = originalWord;
            this.pattern = pattern;
        }

        public String getOriginalWord() { return originalWord; }
        public Pattern getPattern() { return pattern; }
    }

    @PostConstruct
    public void init() {
        reload();
    }

    @Scheduled(fixedDelay = 300_000)
    public void reload() {
        try {
            List<WordFilterEntity> all = wordFilterRepository.findAll();
            synchronized (forbiddenWords) {
                forbiddenWords.clear();
                all.forEach(e -> forbiddenWords.add(e.getWord().toLowerCase()));
            }

            List<CompiledFilter> newStrict = new ArrayList<>();
            List<CompiledFilter> newContext = new ArrayList<>();

            for (WordFilterEntity entity : all) {
                String originalWord = entity.getWord();
                if (originalWord == null || originalWord.trim().isEmpty()) continue;

                String collapsedWord = collapseRepeatedChars(originalWord);
                String regexStr = buildFillerInsensitiveRegex(collapsedWord);

                if (isStrictWord(originalWord)) {
                    Pattern p = Pattern.compile(regexStr, Pattern.CASE_INSENSITIVE);
                    newStrict.add(new CompiledFilter(originalWord, p));
                } else {
                    String contextRegex = "(^|(?<=\\s|\\p{Punct}))" + regexStr + "((?=\\s|\\p{Punct})|$)";
                    Pattern p = Pattern.compile(contextRegex, Pattern.CASE_INSENSITIVE);
                    newContext.add(new CompiledFilter(originalWord, p));
                }
            }

            synchronized (strictFilters) {
                strictFilters.clear();
                strictFilters.addAll(newStrict);
            }
            synchronized (contextFilters) {
                contextFilters.clear();
                contextFilters.addAll(newContext);
            }

            log.info("Word filter reloaded. Total words: {}. Strict: {}. Context: {}", 
                     forbiddenWords.size(), newStrict.size(), newContext.size());
        } catch (Exception e) {
            log.error("Failed to reload word filter: {}", e.getMessage());
        }
    }

    @Transactional
    public void addWord(String word) {
        if (wordFilterRepository.findByWordIgnoreCase(word).isEmpty()) {
            WordFilterEntity entity = new WordFilterEntity();
            entity.setWord(word.toLowerCase());
            wordFilterRepository.save(entity);
        }
        reload();
    }

    @Transactional
    public void removeWord(String word) {
        wordFilterRepository.deleteByWordIgnoreCase(word);
        reload();
    }

    public boolean isForbidden(String content) {
        return findForbiddenWord(content) != null;
    }

    public String findForbiddenWord(String content) {
        if (content == null || content.trim().isEmpty()) return null;

        String sanitized = normalizeArabic(content);
        sanitized = collapseRepeatedChars(sanitized);

        synchronized (strictFilters) {
            for (CompiledFilter filter : strictFilters) {
                if (filter.getPattern().matcher(sanitized).find()) {
                    return filter.getOriginalWord();
                }
            }
        }

        synchronized (contextFilters) {
            for (CompiledFilter filter : contextFilters) {
                if (filter.getPattern().matcher(sanitized).find()) {
                    return filter.getOriginalWord();
                }
            }
        }

        return null;
    }

    public Set<String> getAllWords() {
        synchronized (forbiddenWords) {
            return new HashSet<>(forbiddenWords);
        }
    }

    private String collapseRepeatedChars(String str) {
        if (str == null || str.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        char prev = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c != prev || Character.isWhitespace(c) || !Character.isLetterOrDigit(c)) {
                sb.append(c);
                prev = c;
            }
        }
        return sb.toString();
    }

    private String normalizeArabic(String str) {
        if (str == null) return "";
        String normalized = str.replace("\u0640", "");
        normalized = normalized.replaceAll("[\\u064B-\\u065F\\u0670]", "");
        normalized = normalized.replace('أ', 'a')
                               .replace('إ', 'a')
                               .replace('آ', 'a')
                               .replace('ا', 'a')
                               .replace('ة', 'h')
                               .replace('ه', 'h')
                               .replace('ى', 'y')
                               .replace('ي', 'y');
        return normalized;
    }

    private String buildFillerInsensitiveRegex(String word) {
        String normalized = normalizeArabic(word);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            if (i > 0) {
                regex.append("[\\s_\\-\\+\\.\\*\\u0640]*");
            }
            char c = normalized.charAt(i);
            if ("\\^$.|?*+()[]{}".indexOf(c) >= 0) {
                regex.append('\\').append(c);
            } else {
                regex.append(c);
            }
        }
        return regex.toString();
    }

    public boolean isStrictWord(String word) {
        String normalized = normalizeArabic(word).trim();
        if (normalized.contains("http") || normalized.contains("184.57.51.245") || normalized.contains("1190305586710073427")) {
            return true;
        }
        for (String root : STRICT_ROOTS) {
            String normalizedRoot = normalizeArabic(root);
            if (normalized.contains(normalizedRoot) || normalizedRoot.contains(normalized)) {
                return true;
            }
        }
        return false;
    }
}
