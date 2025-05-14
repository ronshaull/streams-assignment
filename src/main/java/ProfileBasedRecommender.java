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
        // TODO: implement
        //for readability we define the function here for the sort.
        Comparator<T> compareItems = (a, b) -> {
            int result = Double.compare(getItemAverageRating(a.getId()), getItemAverageRating(b.getId()));
            if (result != 0)
                return result;
            result = Integer.compare(getItemRatingsCount(a.getId()), getItemRatingsCount(b.getId()));
            if (result != 0)
                return result;
            if (a.getName().compareTo(b.getName()) > 0) {
                return 1;
            } else {
                return 0;
            }

        };
        // Here we get the ids of the top 10 items. the function we use gives us the Rating of those items,
        // making sure to get only Rating not from our user.
        List<Integer> top10items = getMatchingRatings(getMatchingProfileUsers(userId).stream()
                .map(User::getId)
                .collect(toList())).stream()
                .map(Rating::getItemId).collect(toList());
        // Here we use them to filter the Item objects from the list.
        return items.values().stream()
                .filter(i -> {
                    return top10items.contains(i.getId());
                })
                .collect(toList());
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
            if (u.getGender() != curr.getGender())
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
                .collect(toList());
    }
}
