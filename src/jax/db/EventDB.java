package jax.db;

import static com.google.common.base.Preconditions.checkState;
import static jasonlib.util.Functions.map;
import static jasonlib.util.Utils.parseEnum;
import java.util.List;
import java.util.function.Function;
import jax.model.Event;
import jax.model.Event.Status;
import com.google.common.base.Strings;
import ez.Row;
import ez.Table;

public class EventDB extends JaxDB {

  @Override
  protected Table getTable() {
    return new Table("event")
        .idColumn()
        .column("name", String.class)
        .column("date", String.class)
        .column("time", String.class)
        .column("status", String.class)
        .column("prize", String.class)
        .column("format", String.class)
        .column("format_desc", String.class)
        .column("featured", Boolean.class);
  }

  public void createEvent(String name, String date, String time, String prize, String format, String formatDesc,
      boolean featured) {
    checkState(!Strings.isNullOrEmpty(name));
    db.insert("event", new Row()
        .with("name", name)
        .with("date", date)
        .with("time", time)
        .with("status", Status.REGISTRATION_OPEN)
        .with("prize", prize)
        .with("format", format)
        .with("format_desc", formatDesc)
        .with("featured", featured)
        );
  }

  public Event getFeaturedEvent() {
    Row row = db.selectSingleRow("SELECT * FROM event WHERE featured = TRUE limit 1");
    return row == null ? null : deserializer.apply(row);
  }

  public List<Event> getAll() {
    return map(db.select("SELECT * FROM event"), deserializer);
  }

  public List<Event> getNotCompletedEvents() {
    return map(db.select("SELECT * FROM event WHERE status != 'COMPLETE'"), deserializer);
  }

  public Event get(long id) {
    Row row = db.selectSingleRow("SELECT * FROM event WHERE id = ?", id);
    return deserializer.apply(row);
  }

  public void updateStatus(long id, Status status) {
    db.update("UPDATE event SET status = ? WHERE id = ?", status, id);
  }

  private final Function<Row, Event> deserializer = row -> {
    return new Event(row.getLong("id"),
        row.get("name"),
        row.get("date"),
        row.get("time"),
        parseEnum(row.get("status"), Status.class),
        row.get("prize"),
        row.get("format"),
        row.get("format_desc"),
        row.getBoolean("featured")
      );
    };

}
