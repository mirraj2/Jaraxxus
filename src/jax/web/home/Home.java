package jax.web.home;

import jax.db.EventDB;
import jax.model.Event;
import bowser.Controller;
import bowser.template.Data;

public class Home extends Controller {

  private final EventDB eventDB = new EventDB();

  @Override
  public void init() {
    route("GET", "/").to("home.html").data(eventData);
  }

  private final Data eventData = context -> {
    Event event = eventDB.getFeaturedEvent();
    context.put("event", event);
  };

}
