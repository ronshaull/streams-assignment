import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/** Similarity‑based recommender with bias correction. */
class SimilarityBasedRecommender<T extends Item> extends RecommenderSystem<T> {
    double globalBias;
    Map<Integer, Double> itemsBias;
    Map<Integer, Double> usersBias;
    Map<Integer, List<Double>> itemsRaters; // key=items id, value= users id that rated it.
    // TODO: add data structures to hold the global/item/user biases
    public SimilarityBasedRecommender(Map<Integer, User> users,
                                      Map<Integer, T> items,
                                      List<Rating<T>> ratings) {
        super(users, items, ratings);
        // TODO: initialize the data structures that hold the global/item/user biases
        // lets create those as maps and lIstS.
        globalBias=calcGlobalBias();
//        System.out.println(globalBias);
        itemsBias=items.values().stream()
                .collect(toMap(
                        Item::getId,
                        i-> clacItemBias(i.getId())
                ));
//        itemsBias.forEach((key, value) -> System.out.println(key + " => " + value));

        usersBias = users.values().stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> clacUserBias(u.getId())
                ));
//        usersBias.forEach((key, value) -> System.out.println(key + " => " + value));

    }

    /** Dot‑product similarity; 0 if <10 shared items. */
    public double getSimilarity(int u1, int u2) {
        // TODO: implement
        // first we collect all items that where rated by both users.
        Map<Integer, List<Rating<T>>> itemsBothRated = ratings.stream()
                .filter(r -> r.getUserId() == u1 || r.getUserId() == u2) // keep only ratings from u1 or u2
                .collect(Collectors.groupingBy(
                        Rating::getItemId // group by item ID
                ))
                .entrySet().stream()
                .filter(e -> e.getValue().size() == 2) // keep only items rated by both users
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        if (itemsBothRated.keySet().size() < 10 )
            return 0;
        //for each item in prev collection, we calculate the dot product of bias free value.
        return itemsBothRated.values().stream()
                .mapToDouble(ratingList -> {
                    double r1 = getBiasFreeRating(ratingList.get(0));
                    double r2 = getBiasFreeRating(ratingList.get(1));
                    return r1 * r2;
                })
                .sum();
    }

    @Override
    public List<T> recommendTop10(int userId) {
        // Get top 10 similar users
        List<User> similarUsers = calcTop10Users(userId);
        Set<Integer> similarUserIds = similarUsers.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        // Find items rated by at least 5 of the similar users
        Map<Integer, List<Rating<T>>> ratingsByItem = ratings.stream()
                .filter(r -> similarUserIds.contains(r.getUserId()))
                .collect(groupingBy(Rating::getItemId));

        // Get items rated by at least 5 similar users and NOT rated by the target user
        Set<Integer> itemsRatedByTarget = ratings.stream()
                .filter(r -> r.getUserId() == userId)
                .map(Rating::getItemId)
                .collect(Collectors.toSet());

        Map<T, Double> predictedRatings = ratingsByItem.entrySet().stream()
                .filter(e -> e.getValue().size() >= 5) //items with 5 ratings at least.
                .filter(e -> !itemsRatedByTarget.contains(e.getKey())) // that our user didn't rate.
                .collect(Collectors.toMap(
                        e -> items.get(e.getKey()), // we use it later in sorting.
                        e -> {
                            // we calculate the wieghted avg, each item rating times the user similarity divided by the regular ratings.
                            double[] sum = e.getValue().stream()
                                    .map(r -> {
                                        double sim = getSimilarity(userId, r.getUserId());
                                        double biasFree = getBiasFreeRating(r);
                                        return new double[]{sim * biasFree, Math.abs(sim)}; //sum can be negative, that's what gpt told me.
                                    })
                                    .reduce(new double[]{0.0, 0.0}, (a, b) -> new double[]{
                                            a[0] + b[0], // Sum numerator
                                            a[1] + b[1]  // Sum denominator
                                    });

                            double weightedAvg = sum[1] == 0 ? 0 : sum[0] / sum[1]; // we do it like this so we dont divide by zero by any chance.

                            // Add global, item, and user biases to complete prediction
                            return globalBias
                                    + itemsBias.getOrDefault(e.getKey(), 0.0)
                                    + usersBias.getOrDefault(userId, 0.0)
                                    + weightedAvg;
                            }
                ));
        //now all is left is to sort by weighted avg that we got in predictedRatings.
        return predictedRatings.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // descending order
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(toList());
    }

    // skeleton print functions.
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
    private double calcGlobalBias() {
        return ratings.stream()
                .mapToDouble(Rating::getRating)
                .average().orElse(0.0);// OptionalDouble to double using orElse
    }
    private double clacItemBias(int itemId) {
        return Math.round(ratings.stream()
                .filter(r->r.getItemId()==itemId)
                .mapToDouble(r->r.getRating()-getGlobalBias())
                .average().orElse(0.0)* 1000.0) / 1000.0;
    }
    private double clacUserBias(int userId) {
        return ratings.stream()
                .filter(u->u.getUserId()==userId)
                .mapToDouble(r -> r.getRating() - this.globalBias - getItemBias(r.getItemId()))
                .average().orElse(0.0);
    }

    // those are getters for the value of bias (for Main function).
    public double getGlobalBias() {
        return this.globalBias;
    }
    public double getItemBias(int itemId) {
        return this.itemsBias.getOrDefault(itemId, 0.0);
    }
    public double getUserBias(int userId) {
        return this.usersBias.getOrDefault(userId, 0.0);
    }

    //helper for getSimilarity function
    private double getBiasFreeRating(Rating<T> r) {
        double itemBias = itemsBias.get(r.getItemId());
        double userBias =usersBias.get(r.getUserId());
        return r.getRating() - globalBias - itemBias - userBias;
    }

    // helper for getting top 10 users similar to our user.
    private  List<User> calcTop10Users(int userId) {
        return users.values().stream()
                .collect(toMap(
                        Function.identity(),
                        u-> this.getSimilarity(userId,u.getId())))
                .entrySet().stream()
                .sorted(Map.Entry.<User, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(toList());
    }

}

