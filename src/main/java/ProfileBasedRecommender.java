import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Profile‑based recommender implementation.
 */
class ProfileBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    public ProfileBasedRecommender(Map<Integer, User> users,
                                   Map<Integer, T> items,
                                   List<Rating<T>> ratings) {
        super(users, items, ratings);
    }

    @Override
    public List<T> recommendTop10(int userId) {
        // Step 1: Get similar users
        List<Integer> similarUserIds = getMatchingProfileUsers(userId).stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // Step 2: Get itemIds that current user has rated
        Set<Integer> itemsRatedByCurrentUser = ratings.stream()
                .filter(r -> r.getUserId() == userId)
                .map(Rating::getItemId)
                .collect(Collectors.toSet());

        // Step 3: Group ratings from similar users, excluding items current user rated
        Map<Integer, List<Rating<T>>> ratingsByItem = ratings.stream()
                .filter(r -> similarUserIds.contains(r.getUserId()))
                .filter(r -> !itemsRatedByCurrentUser.contains(r.getItemId()))
                .collect(Collectors.groupingBy(Rating::getItemId));

        // Step 4: Filter items with at least 5 ratings and calculate average
        Map<Integer, Double> qualifiedItemAverages = ratingsByItem.entrySet().stream()
                .filter(e -> e.getValue().size() >= 5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .mapToDouble(Rating::getRating)
                                .average()
                                .orElse(0.0)
                ));

        // Step 5: Create a comparator using similar-user ratings and counts
        Comparator<T> compareItems = (a, b) -> {
            double avgA = qualifiedItemAverages.getOrDefault(a.getId(), 0.0);
            double avgB = qualifiedItemAverages.getOrDefault(b.getId(), 0.0);
            int cmp = Double.compare(avgB, avgA); // Descending average
            if (cmp != 0) return cmp;

            int countA = ratingsByItem.getOrDefault(a.getId(), List.of()).size();
            int countB = ratingsByItem.getOrDefault(b.getId(), List.of()).size();
            cmp = Integer.compare(countB, countA); // Descending count
            if (cmp != 0) return cmp;

            return a.getName().compareTo(b.getName());
        };

        // Step 6: Get top 10 item IDs based on avg ratings
        Set<Integer> topItemIds = qualifiedItemAverages.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // Step 7: Return top 10 items sorted by local comparator
        return items.values().stream()
                .filter(i -> topItemIds.contains(i.getId()))
                .sorted(compareItems)
                .limit(10)
                .collect(Collectors.toList());
    }

    public List<User> getMatchingProfileUsers(int userId) {
        // TODO: implement
        User curr = users.values().stream()
                .filter(u -> u.getId() == userId)
                .findFirst().orElse(null);
        if (curr == null)
            System.out.printf("User %s not found", userId);
        Predicate<User> userPredicate = u -> {
            if (u.getId() == userId)
                return false; // Exclude current user's ratings
            if (!u.getGender().equals(curr.getGender()))
                return false;
            if (Math.abs(u.getAge() - curr.getAge()) > 5)
                return false;
            return true;
        };
        return users.values().stream()
                .filter(userPredicate)
                .collect(toList());
    }

    private List<Rating<T>> getMatchingRatings(List<Integer> userIds) {
        Predicate<Rating<T>> ratingPredicate = r -> {
            if (userIds.contains(r.getUserId()))
                return true;
            else
                return false;
        };
        return ratings.stream()
                .filter(ratingPredicate)
                .collect(Collectors.groupingBy(Rating::getItemId))
                .values().stream()
                .filter(ratingList -> ratingList.size() >= 5)
                .collect(toList()).stream().flatMap(Collection::stream).collect(toList());

    }
}
