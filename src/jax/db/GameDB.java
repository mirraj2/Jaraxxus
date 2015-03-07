package jax.db;

import static com.google.common.base.Preconditions.checkState;
import jasonlib.Json;
import java.time.LocalDateTime;
import java.util.List;
import jax.model.Match;
import jax.model.TournamentManager;
import jax.model.User;
import com.google.common.base.Objects;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import ez.Row;
import ez.Table;

public class GameDB extends JaxDB {

  @Override
  protected Table getTable() {
    return new Table("game")
        .idColumn()
        .column("event", Long.class)
        .column("round", Integer.class)
        .column("match", Long.class)
        .column("date", LocalDateTime.class)
        .column("winnerA", String.class)
        .column("winnerB", String.class);
  }

  public List<Json> getAll() {
    return toJson(db.select("SELECT * FROM game"));
  }

  public List<Json> getGames(long matchId) {
    List<Row> rows = db.select("SELECT winnerA, winnerB FROM game WHERE `match` = ?", matchId);
    return toJson(rows);
  }

  public List<Json> getGames(long eventId, int round) {
    List<Row> rows = db.select("SELECT winnerA, winnerB FROM game WHERE event = ? AND `round` = ?", eventId, round);
    return toJson(rows);
  }

  public Multimap<Long, Json> getGamesByMatch(long eventId) {
    List<Row> rows = db.select("SELECT `match`, winnerA, winnerB FROM game WHERE event = ?", eventId);
    return Multimaps.index(toJson(rows), game -> game.getLong("match"));
  }

  public void recordWinner(Match match, int gameIndex, String winner, User reporter) {
    List<Row> rows = db.select("SELECT * FROM game WHERE `match` = ?", match.id);
    checkState(gameIndex <= rows.size(), gameIndex + " vs " + rows.size());
    checkState(!TournamentManager.BYE.equals(winner), "A BYE can't win a match you silly goose.");

    String winnerA = null, winnerB = null;

    if (reporter.battleTag.equals(match.playerA)) {
      winnerA = winner;
    } else if (reporter.battleTag.equals(match.playerB)) {
      winnerB = winner;
    } else {
      // reported by an admin
      checkState(reporter.isAdmin);

      winnerA = winnerB = winner;
    }

    if (gameIndex == rows.size()) {
      // new row
      Row row = new Row()
          .with("event", match.event)
          .with("round", match.round)
          .with("match", match.id)
          .with("date", LocalDateTime.now())
          .with("winnerA", winnerA)
          .with("winnerB", winnerB);
      db.insert("game", row);
    } else {
      Row row = rows.get(gameIndex);
      if (winnerA != null) {
        if (Objects.equal(row.get("winnerA"), row.get("winnerB"))) {
          if (!reporter.isAdmin) {
            throw new IllegalStateException("You can't change the results once they are agreed upon.");
          }
        }
        row.with("winnerA", winnerA);
      }
      if (winnerB != null) {
        row.with("winnerB", winnerB);
      }
      db.update("game", row);
    }
  }

}
