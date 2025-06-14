package searchengine.services.indexing;

import lombok.NoArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

@Component
@NoArgsConstructor
public class Lemanizer {

    private static final Predicate<String> WORlD_FILTER =
            word -> !word.isBlank() && !word.contains("МЕЖД") && !word.contains("ПРЕДЛ") && !word.contains("СОЮЗ");

    public Map<String, Integer> getLemmaMap(String text) {
        List<String> tokens = getTokens(text);
        if (tokens.isEmpty()) return null;
        Map<String, Integer> lemmaMap = new HashMap<>(tokens.size());
        try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            for (String token : tokens) {
                luceneMorph.getMorphInfo(token).stream()
                        .filter(WORlD_FILTER)
                        .forEach(word -> {
                            List<String> normalForms = luceneMorph.getNormalForms(token);
                            if (!normalForms.isEmpty()) {
                                String normalWord = normalForms.get(0);
                                if (lemmaMap.containsKey(normalWord)) {
                                    lemmaMap.put(normalWord, lemmaMap.get(normalWord) + 1);
                                } else lemmaMap.put(normalWord, 1);
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return lemmaMap;
    }

    private List<String> getTokens(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+"))
                .filter(s -> !s.isBlank() && s.length() > 1).toList();
    }
}
