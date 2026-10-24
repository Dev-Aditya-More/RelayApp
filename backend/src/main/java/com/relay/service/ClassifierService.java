package com.relay.service;

import com.relay.domain.Enums.Category;
import com.relay.domain.Enums.Severity;
import com.relay.web.Dtos.Suggestion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Relay Assist: a lightweight keyword model that suggests a category and severity from the
 * complaint text. It stands in for an image/ML classifier behind the same interface.
 */
@Service
public class ClassifierService {

    private static final Map<Category, List<String>> KEYWORDS = new LinkedHashMap<>();
    private static final List<String> HIGH = List.of("accident", "danger", "dangerous", "huge", "deep", "flood",
            "flooding", "burst", "overflow", "sparking", "live wire", "injur", "urgent", "hospital", "school", "collapsed");
    private static final List<String> LOW = List.of("small", "minor", "slight", "tiny", "faint");

    static {
        KEYWORDS.put(Category.POTHOLE, List.of("pothole", "crater", "road damage", "broken road", "crack", "asphalt", "pit"));
        KEYWORDS.put(Category.GARBAGE, List.of("garbage", "trash", "waste", "dump", "litter", "rubbish", "dustbin", "stink", "smell"));
        KEYWORDS.put(Category.WATER_LEAK, List.of("leak", "water", "pipe", "burst", "tap", "supply", "seepage"));
        KEYWORDS.put(Category.STREETLIGHT, List.of("streetlight", "street light", "lamp", "dark", "bulb", "light pole", "lighting"));
        KEYWORDS.put(Category.DRAINAGE, List.of("drain", "sewage", "sewer", "gutter", "manhole", "clog", "overflow", "waterlogging"));
    }

    public Suggestion classify(String text) {
        String t = text.toLowerCase();
        Category best = Category.OTHER;
        int bestHits = 0;
        List<String> matched = new ArrayList<>();
        for (var e : KEYWORDS.entrySet()) {
            List<String> hits = e.getValue().stream().filter(t::contains).toList();
            if (hits.size() > bestHits) {
                bestHits = hits.size();
                best = e.getKey();
                matched = new ArrayList<>(hits);
            }
        }
        long high = HIGH.stream().filter(t::contains).count();
        long low = LOW.stream().filter(t::contains).count();
        Severity sev = high > 0 ? Severity.HIGH : low > 0 ? Severity.LOW : Severity.MEDIUM;
        int confidence = bestHits == 0 ? 30 : Math.min(95, 55 + bestHits * 15);
        return new Suggestion(best, sev, confidence, matched);
    }

    public String sentiment(String feedback, int rating) {
        if (feedback == null || feedback.isBlank()) return rating >= 4 ? "POSITIVE" : rating <= 2 ? "NEGATIVE" : "NEUTRAL";
        String t = feedback.toLowerCase();
        int score = rating - 3;
        for (String w : List.of("great", "good", "quick", "fast", "thanks", "excellent", "clean", "happy", "perfect"))
            if (t.contains(w)) score++;
        for (String w : List.of("bad", "slow", "poor", "late", "worst", "delay", "incomplete", "rude", "again", "still"))
            if (t.contains(w)) score--;
        return score > 0 ? "POSITIVE" : score < 0 ? "NEGATIVE" : "NEUTRAL";
    }
}
