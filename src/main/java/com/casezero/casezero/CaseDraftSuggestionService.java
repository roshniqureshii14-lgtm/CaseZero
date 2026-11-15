package com.casezero.casezero;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Builds a review-ready case record from publisher metadata.
 * Suggestions are derived only from the article feed; missing details are
 * labelled rather than invented. An administrator still verifies the source.
 */
@Service
public class CaseDraftSuggestionService {

    private static final String NOT_STATED = "Not specified in source";

    private static final List<String> PAKISTAN_LOCATIONS = List.of(
            "Islamabad", "Rawalpindi", "Lahore", "Karachi", "Peshawar", "Quetta",
            "Multan", "Faisalabad", "Gujranwala", "Sialkot", "Hyderabad", "Sukkur",
            "Larkana", "Badah", "Mardan", "Abbottabad", "Swat", "Gwadar", "Turbat",
            "Bahawalpur", "Sargodha", "Sahiwal", "Jhelum", "Mirpur", "Muzaffarabad",
            "Gilgit", "Skardu", "Chaman", "Dera Ismail Khan", "Dera Ghazi Khan",
            "اسلام آباد", "راولپنڈی", "لاہور", "کراچی", "پشاور", "کوئٹہ",
            "ملتان", "فیصل آباد", "گوجرانوالہ", "سیالکوٹ", "حیدرآباد", "سکھر",
            "لاڑکانہ", "مردان", "ایبٹ آباد", "گوادر", "تربت", "بہاولپور");

    public CaseDraftSuggestion suggest(CaseResearchReport report) {
        String title = clean(report.getTitle());
        String description = clean(report.getDescription());
        String combined = (title + " " + description).toLowerCase(Locale.ROOT);

        String category;
        if (containsAny(combined, "unidentified", "unknown body", "unclaimed body",
                "unidentified body", "نامعلوم لاش", "نامعلوم شخص")) {
            category = "Unidentified Persons";
        } else if (containsAny(combined, "missing", "disappeared", "abducted", "kidnapped",
                "لاپتہ", "گمشدہ", "اغوا")) {
            category = "Missing Persons";
        } else if (containsAny(combined, "murder", "killed", "assault", "unsolved",
                "cold case", "قتل", "تشدد")) {
            category = "Unsolved Crimes";
        } else {
            category = "Other Unresolved Cases";
        }

        String location = findLocation(title + " " + description);
        String summary;
        if (!description.isBlank()) {
            summary = description;
        } else {
            summary = "The source headline reports: " + title
                    + ". The publisher feed did not provide a full description. "
                    + "Open the original source to review the complete report.";
        }

        if (summary.length() > 1200) {
            summary = summary.substring(0, 1197).stripTrailing() + "...";
        }

        return new CaseDraftSuggestion(category, location, summary);
    }

    private String findLocation(String text) {
        String lower = clean(text).toLowerCase(Locale.ROOT);
        for (String place : PAKISTAN_LOCATIONS) {
            Pattern pattern = Pattern.compile("(?<![\\p{L}])"
                    + Pattern.quote(place.toLowerCase(Locale.ROOT))
                    + "(?![\\p{L}])");
            Matcher matcher = pattern.matcher(lower);
            if (matcher.find()) return place;
        }
        return NOT_STATED;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private String clean(String value) {
        if (value == null) return "";
        return value.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
