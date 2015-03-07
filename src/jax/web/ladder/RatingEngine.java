package jax.web.ladder;

import static jasonlib.util.Functions.map;
import jasonlib.Json;
import jasonlib.util.Utils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jax.model.Match;
import jax.model.User;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class RatingEngine {

  public List<Json> run(Multimap<Match, Json> matchGames) {
    Map<String, Double> ratings = Maps.newHashMap();

    for (Match match : matchGames.keySet()) {
      if (match.isBye()) {
        continue;
      }

      for (Json game : matchGames.get(match)) {
        String winner = getWinner(game);
        if (winner == null) {
          continue;
        }

        double ratingA = ratings.getOrDefault(match.playerA, 1200d);
        double ratingB = ratings.getOrDefault(match.playerB, 1200d);
        double newA = getNewRating(ratingA, ratingB, winner.equals(match.playerA) ? 1 : 0);
        double newB = getNewRating(ratingB, ratingA, winner.equals(match.playerB) ? 1 : 0);
        ratings.put(match.playerA, newA);
        ratings.put(match.playerB, newB);
      }
    }

    List<String> players = Lists.newArrayList(ratings.keySet());
    Collections.sort(players, (a, b) -> {
      return Utils.signum(ratings.get(b) - ratings.get(a));
    });

    return map(players, player -> {
      return Json.object().with("name", User.prettify(player)).with("rating", Math.round(ratings.get(player)));
    });
  }

  public double getNewRating(double rating, double opponentRating, double score) {
    double kFactor = getKFactor(rating);
    double expectedScore = getExpectedScore(rating, opponentRating);
    return calculateNewRating(rating, score, expectedScore, kFactor);
  }

  private double calculateNewRating(double oldRating, double score, double expectedScore, double kFactor) {
    return oldRating + (kFactor * (score - expectedScore));
  }

  private double getKFactor(double rating) {
    return 32;
  }

  private double getExpectedScore(double rating, double opponentRating) {
    return 1.0 / (1.0 + Math.pow(10.0, ((opponentRating - rating) / 400.0)));
  }

  private String getWinner(Json game) {
    String winnerA = game.get("winnerA");
    String winnerB = game.get("winnerB");
    if (winnerA == null && winnerB == null) {
      return null;
    }
    if (winnerA == null) {
      return winnerB;
    }
    if (winnerB == null) {
      return winnerA;
    }
    if (winnerA.equals(winnerB)) {
      return winnerA;
    }
    return null;
  }

}
