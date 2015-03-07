package jax.db;

import static jasonlib.util.Functions.map;
import jasonlib.Json;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import ez.DB;
import ez.Row;
import ez.Table;

public abstract class JaxDB {

  public static final DB db;

  static {
    db = new DB("localhost", "root", "", "jax");
  }

  public JaxDB() {
    Table table = getTable();
    if (!db.hasTable(table.name)) {
      db.addTable(table);
    }
  }

  protected abstract Table getTable();

  public List<Json> toJson(List<Row> rows) {
    return map(rows, row -> toJson(row));
  }

  public Json toJson(Row row) {
    return toJson(row, ImmutableMap.of());
  }

  public Json toJson(Row row, Map<String, String> keyTransform) {
    Json ret = Json.object();
    row.getMap().forEach((key, value) -> {
      key = keyTransform.getOrDefault(key, key);
      add(ret, key, value);
    });
    return ret;
  }

  private void add(Json json, String key, Object value) {
    if (value == null) {
      return;
    }

    if (value instanceof String) {
      json.with(key, (String) value);
    } else if (value instanceof Number) {
      json.with(key, (Number) value);
    } else if (value instanceof Boolean) {
      json.with(key, (Boolean) value);
    } else if (value instanceof Date) {
      json.with(key, value.toString());
    } else if (value instanceof Timestamp) {
      json.with(key, value.toString());
    } else {
      throw new RuntimeException("Unhandled type: " + value.getClass());
    }
  }

}
