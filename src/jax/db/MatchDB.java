package jax.db;

import static jasonlib.util.Functions.map;
import static jasonlib.util.Functions.splice;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import jax.model.Match;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import ez.Row;
import ez.Table;

public class MatchDB extends JaxDB {

  @Override
  protected Table getTable() {
    return new Table("match")
        .idColumn()
        .column("event", Long.class)
        .column("round", Integer.class)
        .column("playerA", String.class)
        .column("playerB", String.class)
        .column("date", LocalDateTime.class);
  }

  public Match get(long id) {
    return deserializer.apply(db.selectSingleRow("SELECT * FROM `match` WHERE id = ?", id));
  }

  public List<Match> getAll() {
    return map(db.select("SELECT * FROM `match`"), deserializer);
  }

  public List<Match> getMatches(long eventId, long round) {
    List<Row> rows = db.select("SELECT * FROM `match`"
        + " WHERE event = ? AND `round` = ?", eventId, round);
    return map(rows, deserializer);
  }

  public Multimap<Integer, Match> getMatchesByRound(long eventId) {
    List<Row> rows = db.select("SELECT * FROM `match` WHERE event = ?", eventId);
    return Multimaps.index(map(rows, deserializer), match -> match.round);
  }

  public Integer getCurrentRound(long eventId) {
    Row row = db.selectSingleRow("SELECT round FROM `match` WHERE event = ? ORDER BY round DESC LIMIT 1", eventId);
    return row == null ? null : row.getInt("round");
  }

  public void insert(List<Match> matches) {
    List<Row> rows = map(matches, serializer);
    db.insert("match", rows);
    splice(matches, rows, (match, row) -> {
      match.id = row.getLong("id");
    });
  }

  private final Function<Match, Row> serializer = match -> {
    return new Row()
        .with("event", match.event)
        .with("round", match.round)
        .with("playerA", match.playerA)
        .with("playerB", match.playerB)
        .with("date", LocalDateTime.now());
  };

  private final Function<Row, Match> deserializer = row -> {
    return new Match(row.getLong("id"),
        row.getLong("event"),
        row.getInt("round"),
        row.get("playerA"),
        row.get("playerB"));
  };

}
