package com.integrafty.opexy.service;

import com.integrafty.opexy.entity.WordFilterEntity;
import com.integrafty.opexy.entity.WordWhitelistEntity;
import com.integrafty.opexy.repository.WordFilterRepository;
import com.integrafty.opexy.repository.WordWhitelistRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordFilterService {

    private final WordFilterRepository wordFilterRepository;
    private final WordWhitelistRepository wordWhitelistRepository;
    private final List<FilterPattern> filterPatterns = new ArrayList<>();
    private final Set<String> whitelistWords = new HashSet<>();

    private static class FilterPattern {
        private final String originalWord;
        private final Pattern pattern;
        private final boolean strict;

        public FilterPattern(String originalWord, Pattern pattern, boolean strict) {
            this.originalWord = originalWord;
            this.pattern = pattern;
            this.strict = strict;
        }
    }

    @PostConstruct
    public void init() {
        reload();
    }

    @Scheduled(fixedDelay = 300_000)
    public void reload() {
        try {
            List<WordFilterEntity> all = wordFilterRepository.findAll();
            List<WordWhitelistEntity> whitelistAll = wordWhitelistRepository.findAll();
            synchronized (filterPatterns) {
                filterPatterns.clear();
                for (WordFilterEntity entity : all) {
                    FilterPattern fp = compileWord(entity);
                    if (fp != null) {
                        filterPatterns.add(fp);
                    }
                }
            }
            synchronized (whitelistWords) {
                whitelistWords.clear();
                for (WordWhitelistEntity entity : whitelistAll) {
                    whitelistWords.add(sanitize(entity.getWord()));
                }
            }
            log.info("Word filter reloaded. Total patterns: {}, whitelist: {}", filterPatterns.size(), whitelistWords.size());
        } catch (Exception e) {
            log.error("Failed to reload word filter: {}", e.getMessage());
        }
    }

    @Transactional
    public void addWord(String word) {
        addWord(word, false);
    }

    @Transactional
    public void addWord(String word, boolean strict) {
        if (wordFilterRepository.findByWordIgnoreCase(word).isEmpty()) {
            WordFilterEntity entity = new WordFilterEntity();
            entity.setWord(word.toLowerCase());
            entity.setStrict(strict);
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
        if (content == null || content.isEmpty()) {
            return null;
        }
        String sanitizedContent = sanitize(content);
        synchronized (filterPatterns) {
            for (FilterPattern fp : filterPatterns) {
                Matcher matcher = fp.pattern.matcher(sanitizedContent);
                if (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();
                    int tokenStart = start;
                    while (tokenStart > 0 && !isBoundaryChar(sanitizedContent.charAt(tokenStart - 1))) {
                        tokenStart--;
                    }
                    int tokenEnd = end;
                    while (tokenEnd < sanitizedContent.length() && !isBoundaryChar(sanitizedContent.charAt(tokenEnd))) {
                        tokenEnd++;
                    }
                    String token = sanitizedContent.substring(tokenStart, tokenEnd);
                    if (isWhitelisted(token)) {
                        continue;
                    }
                    return fp.originalWord;
                }
            }
        }
        return null;
    }

    private boolean isBoundaryChar(char c) {
        return Character.isWhitespace(c) || Pattern.matches("\\p{Punct}", String.valueOf(c));
    }

    private boolean isWhitelisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String sanitized = sanitize(token);
        synchronized (whitelistWords) {
            for (String whitelisted : whitelistWords) {
                if (sanitized.contains(whitelisted)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<String> getAllWords() {
        Set<String> words = new HashSet<>();
        synchronized (filterPatterns) {
            for (FilterPattern fp : filterPatterns) {
                words.add(fp.originalWord);
            }
        }
        return words;
    }

    public boolean isStrict(String word) {
        if (word == null) {
            return false;
        }
        synchronized (filterPatterns) {
            for (FilterPattern fp : filterPatterns) {
                if (fp.originalWord.equalsIgnoreCase(word)) {
                    return fp.strict;
                }
            }
        }
        return false;
    }

    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= 0x064B && c <= 0x065F) {
                continue;
            }
            if (c == '\u0640') {
                continue;
            }
            if (c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ') {
                c = 'ا';
            } else if (c == 'ة') {
                c = 'ه';
            } else if (c == 'ى' || c == 'ئ') {
                c = 'ي';
            } else if (c == 'ؤ') {
                c = 'و';
            }
            sb.append(c);
        }
        String result = sb.toString();
        result = result.replaceAll("(.)\\1+", "$1");
        return result;
    }

    private FilterPattern compileWord(WordFilterEntity entity) {
        String originalWord = entity.getWord();
        String sanitized = sanitize(originalWord);
        if (sanitized.isEmpty()) {
            return null;
        }
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < sanitized.length(); i++) {
            char c = sanitized.charAt(i);
            regex.append(Pattern.quote(String.valueOf(c)));
            if (i < sanitized.length() - 1) {
                regex.append("[\\s_\\-\\+\\.\\*\\u0640]*");
            }
        }
        boolean strict = entity.isStrict();
        String finalRegex;
        if (strict) {
            finalRegex = regex.toString();
        } else {
            finalRegex = "(?:^|\\s|[\\p{Punct}])(" + regex.toString() + ")(?:$|\\s|[\\p{Punct}])";
        }
        Pattern pattern = Pattern.compile(finalRegex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return new FilterPattern(originalWord, pattern, strict);
    }
}
