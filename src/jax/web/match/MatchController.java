package jax.web.match;

import static com.google.common.base.Preconditions.checkState;
import jasonlib.Json;
import java.util.List;
import jax.db.EventDB;
import jax.db.GameDB;
import jax.db.MatchDB;
import jax.model.Event;
import jax.model.Match;
import jax.model.TournamentManager;
import jax.model.User;
import bowser.Controller;
import bowser.Handler;
import bowser.template.Data;

public class MatchController extends Controller {

  private final EventDB eventDB = new EventDB();
  private final MatchDB matchDB = new MatchDB();
  private final GameDB gameDB = new GameDB();
  private final TournamentManager tournamentManager = new TournamentManager();

  @Override
  public void init() {
    route("GET", "/match/*").to("match.html").data(matchData);
    route("POST", "/match/*/result/*").to(resultHandler);
  }

  private final Handler resultHandler = (request, response) -> {
    int matchId = request.getInt(1);
    int gameIndex = request.getInt(3) - 1;

    Match match = matchDB.get(matchId);

    User user = request.get("user");

    checkState(user.isAdmin
        || user.battleTag.equals(match.playerA)
        || user.battleTag.equals(match.playerB));

    String winner = request.param("winner");
    checkState(winner.equals(match.playerA) || winner.equals(match.playerB));

    tournamentManager.recordWinner(match, gameIndex, winner, user);
  };

  private final Data matchData = context -> {
    User user = context.get("user");

    Match match = matchDB.get(context.request.getInt(1));
    Event event = eventDB.get(match.event);

    List<Json> games = gameDB.getGames(match.id);
    if (games.isEmpty()) {
      games.add(Json.object().with("index", 1));
    } else {
      for (int i = 0; i < games.size(); i++) {
        games.get(i).with("index", i + 1);
      }
    }
    context.put("games", games);
    context.put("event", event);
    context.put("match", match);

    if (match.isBye()) {
      context.put("matchTitle", match.playerANick + " has a Bye");
    } else {
      if (user != null && user.isAdmin) {
        context.put("matchTitle", match.playerA + " vs " + match.playerB);
      } else {
        context.put("matchTitle", match.playerANick + " vs " + match.playerBNick);
      }
    }

    if (user != null) {
      boolean iAmPlayerA = match.playerA.equals(user.battleTag);
      boolean iAmPlayerB = match.playerB.equals(user.battleTag);

      context.put("editable", user.isAdmin || iAmPlayerA || iAmPlayerB);

      if (iAmPlayerA || iAmPlayerB) {
        context.put("opponent", iAmPlayerA ? match.playerB : match.playerA);
      }
    }
  };

}
