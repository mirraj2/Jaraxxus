package jax.model;

public class Event {

  public final long id;
  public final String name, date, time, prize, format, format_desc;
  public final Status status;
  public final boolean featured;

  public Event(long id, String name, String date, String time, Status status, String prize, String format,
      String format_desc, boolean featured) {
    this.id = id;
    this.name = name;
    this.date = date;
    this.time = time;
    this.status = status;
    this.prize = prize;
    this.format = format;
    this.format_desc = format_desc;
    this.featured = featured;
  }

  public boolean canJoin() {
    return status == Status.REGISTRATION_OPEN;
  }

  public boolean canLeave() {
    return status == Status.REGISTRATION_OPEN || status == Status.REGISTRATION_CLOSED;
  }

  public boolean is(Status... statuses) {
    for (Status s : statuses) {
      if (this.status == s) {
        return true;
      }
    }
    return false;
  }

  public static enum Status {
    REGISTRATION_OPEN, REGISTRATION_CLOSED, IN_PROGRESS, COMPLETE;

    private static final String[] displayNames =
    { "Registration Open", "Registration Closed", "In Progress", "Complete" };

    @Override
    public String toString() {
      return displayNames[ordinal()];
    };
  }

}
