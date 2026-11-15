package com.casezero.casezero;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class CaseDraftSuggestionService {

    private static final List<String> PAKISTAN_LOCATIONS = List.of(
            "Islamabad", "Rawalpindi", "Lahore", "Karachi", "Peshawar", "Quetta",
            "Multan", "Faisalabad", "Gujranwala", "Sialkot", "Hyderabad", "Sukkur",
            "Larkana", "Badah", "Mardan", "Abbottabad", "Swat", "Gwadar", "Turbat",
            "Bahawalpur", "Sargodha", "Sahiwal", "Jhelum", "Mirpur", "Muzaffarabad",
            "Gilgit", "Skardu", "Chaman", "Dera Ismail Khan", "Dera Ghazi Khan");

    public CaseDraftSuggestion suggest(CaseResearchReport report) {
        String title = safe(report.getTitle());
        String description = safe(report.getDescription());
        String text = (title + " " + description).toLowerCase(Locale.ROOT);

        String category;
        if (containsAny(text, "unidentified", "unknown body", "unclaimed body",
                "unidentified body", "نامعلوم لاش", "نامعلوم شخص")) {
            category = "Unidentified Persons";
        } else if (containsAny(text, "missing", "disappeared", "abducted", "kidnapped",
                "لاپتہ", "گمشدہ", "اغوا")) {
            category = "Missing Persons";
        } else if (containsAny(text, "murder", "killed", "assault", "unsolved",
                "cold case", "قتل", "تشدد")) {
            category = "Unsolved Crimes";
        } else {
            category = "Other Unresolved Cases";
        }

        String location = findLocation(title + " " + description);
        String summary = description.isBlank()
                ? "Source headline: " + title + ". Review the original report and add a neutral summary supported by the source."
                : description.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        if (summary.length() > 1200) summary = summary.substring(0, 1200);

        return new CaseDraftSuggestion(category, location, summary);
    }

    private String findLocation(String text) {
        String lower = safe(text).toLowerCase(Locale.ROOT);
        for (String place : PAKISTAN_LOCATIONS) {
            Pattern pattern = Pattern.compile("(?<![\\p{L}])" + Pattern.quote(place.toLowerCase(Locale.ROOT))
                    + "(?![\\p{L}])");
            Matcher matcher = pattern.matcher(lower);
            if (matcher.find()) return place;
        }
        return "";
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}