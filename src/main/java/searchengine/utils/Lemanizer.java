package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

@Component
public class Lemanizer {

    private final LuceneMorphology russianLuceneMorph;
    private final LuceneMorphology englishLuceneMorph;

    private static final Predicate<String> RUSSIAN_WORlD_FILTER =
            word -> !word.isBlank()
                    && !word.contains("МЕЖД")
                    && !word.contains("ПРЕДЛ")
                    && !word.contains("СОЮЗ");
    private static final Predicate<String> ENGLISH_WORlD_FILTER =
            word -> !word.isBlank()
                    && !word.contains("PN")
                    && !word.contains("PN_ADJ")
                    && !word.contains("PRON")
                    && !word.contains("ARTICLE")
                    && !word.contains("VERB prsa,sg,3")
                    && !word.contains("NOUN prop,f,sg")
                    && !word.contains("CONJ");

    public Lemanizer() {
        try {
            russianLuceneMorph = new RussianLuceneMorphology();
            englishLuceneMorph = new EnglishLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Integer> getLemmaMap(String text) {
        List<String> tokens = getTokens(text);
        if (tokens.isEmpty()) return new HashMap<>(0);
        Map<String, Integer> lemmaMap = new HashMap<>(tokens.size());
        try {
            for (String token : tokens) {
                addLemmas(lemmaMap, token);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return lemmaMap;
    }

    public Set<String> getLemmaSet(String text) {
        Map<String, Integer> lemmaMap = getLemmaMap(text);
        if (lemmaMap.isEmpty()) return new HashSet<>(0);
        return lemmaMap.keySet();
    }

    private void addLemmas(Map<String, Integer> lemmaMap, String token) throws IOException {
        if (token.matches("^[а-яА-Я]+$")) {
            russianLuceneMorph.getMorphInfo(token).stream()
                    .filter(RUSSIAN_WORlD_FILTER)
                    .forEach(word -> {
                        List<String> normalForms = russianLuceneMorph.getNormalForms(token);
                        if (!normalForms.isEmpty()) {
                            String normalWord = normalForms.get(0);
                            if (lemmaMap.containsKey(normalWord)) {
                                lemmaMap.put(normalWord, lemmaMap.get(normalWord) + 1);
                            } else lemmaMap.put(normalWord, 1);
                        }
                    });
        }
        if (token.matches("^[a-zA-Z]+$")) {
            englishLuceneMorph.getMorphInfo(token).stream()
                    .filter(ENGLISH_WORlD_FILTER)
                    .forEach(word -> {
                        List<String> normalForms = englishLuceneMorph.getNormalForms(token);
                        if (!normalForms.isEmpty()) {
                            String normalWord = normalForms.get(0);
                            if (lemmaMap.containsKey(normalWord)) {
                                lemmaMap.put(normalWord, lemmaMap.get(normalWord) + 1);
                            } else lemmaMap.put(normalWord, 1);
                        }
                    });
        }
    }

    private List<String> getTokens(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яА-Яa-zA-Z\\s])", " ")
                .trim()
                .split("\\s+"))
                .filter(s -> !s.isBlank() && s.length() > 1).toList();
    }
}
