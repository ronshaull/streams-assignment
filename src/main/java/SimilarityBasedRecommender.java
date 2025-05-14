import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

/** Similarity‑based recommender with bias correction. */
class SimilarityBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    // TODO: add data structures to hold the global/item/user biases
    public SimilarityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);
        // TODO: initialize the data structures that hold the global/item/user biases
    }

    /** Dot‑product similarity; 0 if <10 shared items. */
    public double getSimilarity(int u1, int u2) {
        // TODO: implement
        double similarity =
        return 0;
    }

    @Override public List<T> recommendTop10(int userId){
        // TODO: implement
        return null;
    }

    public void printGlobalBias() {
        // TODO: fix
        // avg of all ratings.
        System.out.println("Global bias: " + getGlobalBias());
    }
    public void printItemBias(int itemId) {
        // TODO: fix
        // each item rating minus global bias avg.
        System.out.println("Item bias for item " + itemId + ": " + getItemBias(itemId));
    }
    public void printUserBias(int userId) {
        // TODO: fix
        System.out.println("User bias for user " + userId + ": " + getUserBias(userId));
    }

    // we add those function as helpers:
    private double getGlobalBias() {
        return ratings.stream()
                .mapToDouble(Rating::getRating)
                .average().orElse(0.0);// OptionalDouble to double using orElse
    }
    private double getItemBias(int itemId) {
        return ratings.stream()
                .filter(r->r.getItemId()==itemId)
                .mapToDouble(r->r.getRating()-getGlobalBias())
                .average().orElse(0.0);
    }
    private double getUserBias(int userId) {
        double globalBias = getGlobalBias(); // so we don't calculate it everytime.
        return ratings.stream()
                .filter(u->u.getUserId()==userId)
                .mapToDouble(r->r.getRating()-globalBias-getItemBias(r.getItemId()))
                .average().orElse(0.0);
    }
}

