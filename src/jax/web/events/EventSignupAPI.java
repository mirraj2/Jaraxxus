package jax.web.events;

import static com.google.common.base.Preconditions.checkState;
import jasonlib.Log;
import jax.db.EventDB;
import jax.db.SignupDB;
import jax.model.Event;
import jax.model.User;
import bowser.Controller;
import bowser.Handler;

public class EventSignupAPI extends Controller {

  private final SignupDB signupDB = new SignupDB();
  private final EventDB eventDB = new EventDB();

  @Override
  public void init() {
    route("GET", "/events/*/join").to(join);
    route("GET", "/events/*/leave").to(leave);
  }

  private final Handler join = (request, response) -> {
    int eventId = request.getInt(1);
    Event event = eventDB.get(eventId);

    checkState(event.canJoin());

    User user = request.get("user");
    
    Log.debug(user.battleTag + " is registering for event " + eventId);
    signupDB.register(user.battleId, eventId);

    response.redirect("/events/" + eventId);
  };

  private final Handler leave = (request, response) -> {
    int eventId = request.getInt(1);
    Event event = eventDB.get(eventId);

    checkState(event.canLeave());

    User user = request.get("user");

    Log.debug(user.battleTag + " is leaving event " + eventId);
    signupDB.unregister(user.battleId, eventId);

    response.redirect("/events/" + eventId);
  };

}
