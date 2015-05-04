package jax.db;

import java.time.LocalDateTime;
import ez.Row;
import ez.Table;

public class VersionDB extends JaxDB {

  @Override
  protected Table getTable() {
    return new Table("version")
        .idColumn()
        .column("date", LocalDateTime.class);
  }

  public int getCurrentVersion() {
    Row row = db.selectSingleRow("SELECT id FROM version ORDER BY id desc LIMIT 1");
    return row == null ? 0 : row.getLong("id").intValue();
  }

  public void incVersion() {
    db.insert("version", new Row().with("date", LocalDateTime.now()));
  }

}
