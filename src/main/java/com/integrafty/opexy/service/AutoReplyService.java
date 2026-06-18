package com.integrafty.opexy.service;

import com.integrafty.opexy.entity.AutoReplyEntity;
import com.integrafty.opexy.repository.AutoReplyRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AutoReplyService {
    private static final Logger log = LoggerFactory.getLogger(AutoReplyService.class);

    private final AutoReplyRepository autoReplyRepository;

    private final Map<String, String> cache = new HashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    @Scheduled(fixedDelay = 600_000)
    public void refreshCache() {
        try {
            List<AutoReplyEntity> all = autoReplyRepository.findAll();
            synchronized (cache) {
                cache.clear();
                all.forEach(e -> cache.put(e.getKeyword().toLowerCase(), e.getResponseText()));
            }
            log.info("Auto-replies reloaded. Total pairs: {}", cache.size());
        } catch (Exception e) {
            log.error("Failed to reload auto-replies: {}", e.getMessage());
        }
    }

    @Transactional
    public void addResponse(String keyword, String responseText, String addedBy) {
        AutoReplyEntity entity = autoReplyRepository
                .findByKeywordIgnoreCase(keyword)
                .orElse(new AutoReplyEntity());
        entity.setKeyword(keyword.toLowerCase());
        entity.setResponseText(responseText);
        entity.setAddedBy(addedBy);
        autoReplyRepository.save(entity);
        refreshCache();
    }

    @Transactional
    public void removeResponse(String keyword) {
        autoReplyRepository.deleteByKeywordIgnoreCase(keyword);
        refreshCache();
    }

    public Map<String, String> getAllResponses() {
        synchronized (cache) {
            return new HashMap<>(cache);
        }
    }

    public String getResponse(String content) {
        String lower = content.toLowerCase();
        synchronized (cache) {
            for (Map.Entry<String, String> entry : cache.entrySet()) {
                String keyword = entry.getKey();
                if (lower.matches("(^|.*\\s)" + java.util.regex.Pattern.quote(keyword) + "(\\s.*|$)")) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}
