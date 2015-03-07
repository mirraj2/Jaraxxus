package jax.db;

import static jasonlib.util.Functions.map;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import jax.model.User;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import ez.Row;
import ez.Table;

public class UserDB extends JaxDB {

  @Override
  protected Table getTable() {
    return new Table("user")
        .idColumn()
        .column("battleId", Integer.class)
        .column("join_date", LocalDateTime.class)
        .column("battleTag", String.class)
        .column("admin", Boolean.class);
  }

  public User get(int battleId) {
    Row row = db.selectSingleRow("SELECT * FROM user WHERE battleId = ?", battleId);
    return row == null ? null : deserializer.apply(row);
  }

  public void createUser(User user) {
    db.insert("user", new Row()
        .with("battleId", user.battleId)
        .with("join_date", LocalDateTime.now())
        .with("battleTag", user.battleTag)
        .with("admin", user.isAdmin)
        );
  }

  public List<User> getUsers(List<Integer> battleIds) {
    if (battleIds.isEmpty()) {
      return ImmutableList.of();
    }
    List<User> users = map(db.select("SELECT * FROM user WHERE battleId IN (" + Joiner.on(",").join(battleIds) + ")"),
        deserializer);
    Map<Integer, User> index = Maps.uniqueIndex(users, user -> user.battleId);
    return map(battleIds, id -> index.get(id));
  }

  private final Function<Row, User> deserializer = row -> {
    return new User(row.getInt("battleId"), row.get("battleTag"), row.getBoolean("admin"));
  };

}
