package com.example.trackmate.utils;

import com.example.trackmate.models.ReportedItem;
import java.util.ArrayList;
import java.util.List;

public class SearchUtils {
    public static List<ReportedItem> filterItems(List<ReportedItem> items, String query) {
        List<ReportedItem> filteredList = new ArrayList<>();
        String searchQuery = query.toLowerCase().trim();

        for (ReportedItem item : items) {
            if (itemMatchesQuery(item, searchQuery)) {
                filteredList.add(item);
            }
        }
        return filteredList;
    }

    private static boolean itemMatchesQuery(ReportedItem item, String query) {
        // Check if any of the item fields contain the search query
        return (item.getName() != null && item.getName().toLowerCase().contains(query)) ||
               (item.getDescription() != null && item.getDescription().toLowerCase().contains(query)) ||
               (item.getLocation() != null && item.getLocation().toLowerCase().contains(query)) ||
               (item.getDate() != null && item.getDate().toLowerCase().contains(query));
    }
}