import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.*;

/** Popularity‑based recommender implementation. */
class PopularityBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    private static final int POPULARITY_THRESHOLD = 100;
    public PopularityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);
    }

    @Override
    public List<T> recommendTop10(int userId)  {
        // TODO: implement
        // for better readability we define the compare function here.
        Comparator<T> compareItems=(a,b)->{
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
        List<T> result = items.values().stream()
                    .filter(r -> {return getItemRatingsCount(r.getId()) > 100 && !isRatedBy(r.getId(), userId);})
                    .sorted(compareItems)
                    .limit(10)
                    .collect(toList());
            return null;
    }

    public double getItemAverageRating(int itemId) {
        // TODO: implement
            return ratings.stream()
                    .filter(r->r.getItemId()==itemId)
                    .mapToDouble(Rating::getRating)  //could used average here. takes no args.
                    .reduce(0, Double::sum)/getItemRatingsCount(itemId);
    }
    public int getItemRatingsCount(int itemId)   {
        // TODO: implement

            return Math.toIntExact( ratings.stream()
                    .filter(r->r.getItemId()==itemId)
                    .count());
    }
    public boolean isRatedBy(int itemId, int userId) {
        return ratings.stream()
                .anyMatch(r->r.getItemId()==itemId && r.getUserId()==userId);
    }
}
