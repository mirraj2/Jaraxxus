package jax.model;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;
import static jasonlib.util.Functions.map;
import jasonlib.Json;
import jasonlib.Log;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jax.db.EventDB;
import jax.db.GameDB;
import jax.db.MatchDB;
import jax.db.SignupDB;
import jax.db.UserDB;
import jax.web.chat.JaraxxusSocketServer;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class TournamentManager {

  // put byes in once <= 64 players

  public static final String BYE = "BYE";
  private static final User SYSTEM_USER = new User(-1, "System", true);

  private final EventDB eventDB = new EventDB();
  private final SignupDB signupDB = new SignupDB();
  private final UserDB userDB = new UserDB();
  private final MatchDB matchDB = new MatchDB();
  private final GameDB gameDB = new GameDB();

  public synchronized void recordWinner(Match match, int gameIndex, String winner, User reporter) {
    long eventId = match.event;

    Event event = eventDB.get(eventId);
    checkState(event.is(Event.Status.IN_PROGRESS), "This event is not currently in progress.");

    Integer currentRound = matchDB.getCurrentRound(eventId);

    checkState(match.round == currentRound, "Can't modify a previous round.");

    Log.info(reporter + " has reported that the winner of match " + match.id
        + " game " + (gameIndex + 1) + " is " + winner);

    gameDB.recordWinner(match, gameIndex, winner, reporter);

    if (isRoundOver(match.event, match.round)) {
      Log.info("Round " + match.round + " is complete.");

      moveToNextRound(eventId);
    }
  }

  private boolean isRoundOver(long eventId, int round) {
    List<Match> matches = matchDB.getMatches(eventId, round);
    Multimap<Long, Json> matchGames = gameDB.getGamesByMatch(eventId);

    for (Match match : matches) {
      Collection<Json> games = matchGames.get(match.id);
      if (games.isEmpty()) {
        return false;
      }

      for (Json game : games) {
        // see if any are contested
        String winA = game.get("winnerA");
        String winB = game.get("winnerB");
        if (winA != null && winB != null && !winA.equals(winB)) {
          return false;
        }
      }
    }

    return true;
  }

  public void moveToNextRound(long event) {
    // take the winners of the last round and move them on

    int currentRound = matchDB.getCurrentRound(event);

    List<Json> games = gameDB.getGames(event, currentRound);

    Set<String> winners = Sets.newHashSet();
    for (Json game : games) {
      winners.add(game.get("winnerA"));
      winners.add(game.get("winnerB"));
    }
    winners.remove(null);

    List<String> winnersList = Lists.newArrayList(winners);

    if (winnersList.size() <= 1) {
      // the tournament is over!
      Log.info("The event has completed!");
      eventDB.updateStatus(event, Event.Status.COMPLETE);

      JaraxxusSocketServer.instance.announce("Congratulations to " + User.prettify(winnersList.get(0))
          + " for winning the tournament!");
    } else {
      generateMatches(event, currentRound + 1, winnersList);
    }
  }

  public void startTournament(Event event) {
    Integer currentRound = matchDB.getCurrentRound(event.id);
    if (currentRound != null) {
      return; // this tournament was already started.
    }

    Collection<String> readyPlayers = ImmutableSet.copyOf(JaraxxusSocketServer.instance.readyPlayers.get(event.id));

    List<Integer> playerIds = signupDB.getPlayersSignedUpFor(event.id);
    List<String> players = map(userDB.getUsers(playerIds), user -> user.battleTag);
    
    // for (int i = players.size() - 1; i >= 0; i--) {
    // if (!readyPlayers.contains(players.get(i))) {
    // Log.info(players.get(i) + " was not ready and was auto-kicked.");
    // players.remove(i);
    // }
    // }

    generateMatches(event.id, 1, players);
  }

  private void generateMatches(long eventId, int round, List<String> players) {
    Collections.shuffle(players);

    Map<String, String> pairings = Maps.newLinkedHashMap();

    int numByes;
    if (players.size() > 64) {
      numByes = players.size() % 2;
    } else {
      numByes = roundPowerOfTwoUp(players.size()) - players.size();
    }

    int playersWithoutByes = players.size() - numByes;

    if (players.size() == 1) {
      numByes = 1;
      playersWithoutByes = 0;
    }

    for (int i = 0; i < playersWithoutByes; i += 2) {
      pairings.put(players.get(i), players.get(i + 1));
    }

    for (int i = playersWithoutByes; i < players.size(); i++) {
      pairings.put(players.get(i), BYE);
    }

    List<Match> matches = Lists.newArrayList();
    pairings.forEach((a, b) -> {
      matches.add(new Match(null, eventId, round, a, b));
    });
    matchDB.insert(matches);

    for (Match match : matches) {
      if (match.isBye()) {
        gameDB.recordWinner(match, 0, match.playerA, SYSTEM_USER);
      }
    }

    try {
      JaraxxusSocketServer.instance.pairingsAreUp(eventId, round, matches);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Object getRoundData(long eventId) {
    Integer currentRound = matchDB.getCurrentRound(eventId);
    if (currentRound == null) {
      return null;
    }

    Multimap<Integer, Match> matches = matchDB.getMatchesByRound(eventId);
    Multimap<Long, Json> gamesByMatch = gameDB.getGamesByMatch(eventId);

    Json ret = Json.array();
    for (int i = 1; i <= currentRound; i++) {
      Json round = Json.object().with("num", i);
      ret.add(round);
      round.with("matches", Json.array(matches.get(i), match -> {
        Json matchObject = Json.object();

        matchObject.with("id", match.id);

        String matchName = User.prettify(match.playerA) + " vs " + User.prettify(match.playerB);
        matchObject.with("name", matchName);

        Collection<Json> games = gamesByMatch.get(match.id);
        matchObject.with("status", getStatus(games));

        return matchObject;
      }));
    }
    return ret;
  }

  public String getStatus(Collection<Json> games) {
    if (games.size() == 0) {
      return "In Progress";
    }

    checkState(games.size() == 1, "This function only supports one game per round.");

    Json game = getOnlyElement(games);

    String winnerA = game.get("winnerA");
    String winnerB = game.get("winnerB");

    if (winnerA == null) {
      return User.prettify(winnerB) + " won";
    } else if (winnerB == null) {
      return User.prettify(winnerA) + " won";
    } else if (winnerA.equals(winnerB)) {
      return User.prettify(winnerA) + " won";
    } else {
      return "Contested";
    }
  }

  private static int roundPowerOfTwoUp(int n) {
    n--;
    n |= n >> 1;
    n |= n >> 2;
    n |= n >> 4;
    n |= n >> 8;
    n |= n >> 16;
    n++;
    return n;
  }

}
