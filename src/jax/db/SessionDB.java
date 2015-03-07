package jax.db;

import java.util.UUID;
import ez.Row;
import ez.Table;

//todo, make these tokens expire
public class SessionDB extends JaxDB {

  @Override
  protected Table getTable() {
    return new Table("session")
        .primary("token", UUID.class)
        .column("battleId", Integer.class)
        .column("blizzToken", String.class);
  }

  public String newSession(int battleId, String blizzToken) {
    UUID token = UUID.randomUUID();

    db.insert("session", new Row()
        .with("token", token)
        .with("battleId", battleId)
        .with("blizzToken", blizzToken));

    return token.toString();
  }

  public Integer getBattleId(String token) {
    Row row = db.selectSingleRow("SELECT battleId FROM session WHERE token = ?", token);
    return row == null ? null : row.getInt("battleId");
  }

}
