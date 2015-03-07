package jax.web.ladder;

import jasonlib.Json;
import java.util.List;
import jax.db.GameDB;
import jax.db.MatchDB;
import jax.model.Match;
import bowser.Controller;
import bowser.template.Data;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class LadderController extends Controller {

  private final MatchDB matchDB = new MatchDB();
  private final GameDB gameDB = new GameDB();

  @Override
  public void init() {
    route("GET", "/ladder").to("ladder.html").data(data);
  }

  private final Data data = context -> {
    List<Match> matches = matchDB.getAll();
    List<Json> games = gameDB.getAll();

    Multimap<Long, Json> idGames = Multimaps.index(games, game -> game.getLong("match"));
    Multimap<Match, Json> matchGames = LinkedListMultimap.create();
    for (Match match : matches) {
      matchGames.putAll(match, idGames.get(match.id));
    }

    List<Json> players = new RatingEngine().run(matchGames);
    context.put("players", players);
  };

}
