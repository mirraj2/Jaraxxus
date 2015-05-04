package jax.db;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toSet;
import jasonlib.Log;
import java.time.LocalDateTime;
import java.util.Set;
import jax.model.Player;
import jax.model.User;
import ez.Row;
import ez.Table;

public class BanDB extends JaxDB {

  @Override
  protected Table getTable() {
    return new Table("ban")
        .idColumn()
        .column("date", LocalDateTime.class)
        .column("ip", String.class)
        .column("battleId", Integer.class)
        .column("battleTag", String.class);
  }

  public Set<String> getBannedIPs() {
    return db.select("SELECT ip FROM ban WHERE battleId IS NULL")
        .stream().map(row -> row.get("ip")).collect(toSet());
  }

  public Set<Integer> getBannedBattleIds() {
    return db.select("SELECT battleId FROM ban WHERE battleId IS NOT NULL")
        .stream().map(row -> row.getInt("battleId")).collect(toSet());
  }

  public void ban(Player player) {
    Log.info("Banning " + player.getName());

    if (player.user == null) {
      banIP(player.ip);
    } else {
      ban(player.user, player.getIP());
    }
  }

  private void banIP(String ip) {
    Row row = db.selectSingleRow("SELECT ip FROM ban WHERE ip = ? AND battleId IS NULL", ip);
    checkState(row == null, "This ip is already banned.");

    db.insert("ban", new Row()
        .with("date", LocalDateTime.now())
        .with("ip", ip)
        .with("battleId", null)
        .with("battleTag", null)
        );
  }

  private void ban(User user, String ip) {
    Row row = db.selectSingleRow("SELECT battleId FROM ban WHERE battleId = ?", user.battleId);
    checkState(row == null, "This battleid is already banned.");

    db.insert("ban", new Row()
        .with("date", LocalDateTime.now())
        .with("ip", ip)
        .with("battleId", user.battleId)
        .with("battleTag", user.battleTag)
        );
  }

}
