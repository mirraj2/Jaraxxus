package jax.db;

import static jasonlib.util.Functions.map;
import java.util.List;
import ez.Row;
import ez.Table;

public class SignupDB extends JaxDB {

  @Override
  protected Table getTable() {
    return new Table("signup")
        .idColumn()
        .column("event", Long.class)
        .column("battleId", Integer.class);
  }

  public void register(int battleId, long eventId) {
    unregister(battleId, eventId);
    db.insert("signup",
        new Row().with("event", eventId).with("battleId", battleId));
  }

  public void unregister(int battleId, long eventId) {
    db.update("DELETE FROM signup WHERE event = ? AND battleId = ?", eventId, battleId);
  }

  public boolean isRegistered(int battleId, long eventId) {
    Row row = db.selectSingleRow("SELECT id FROM signup WHERE event = ? AND battleId = ?", eventId, battleId);
    return row != null;
  }

  public List<Integer> getPlayersSignedUpFor(long eventId) {
    return map(db.select("SELECT battleId FROM signup WHERE event = ?", eventId), row -> row.getInt("battleId"));
  }

}
