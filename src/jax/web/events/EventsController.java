package jax.web.events;

import static com.google.common.base.Preconditions.checkState;
import static jasonlib.util.Utils.parseEnum;
import static java.lang.Integer.parseInt;
import jasonlib.Log;
import java.time.LocalDateTime;
import java.util.List;
import jax.db.EventDB;
import jax.db.JaxDB;
import jax.db.SignupDB;
import jax.db.UserDB;
import jax.model.Event;
import jax.model.Event.Status;
import jax.model.TournamentManager;
import jax.model.User;
import bowser.Controller;
import bowser.Handler;
import bowser.template.Data;
import com.google.common.collect.Lists;

public class EventsController extends Controller {

  private final UserDB userDB = new UserDB();
  private final EventDB eventDB = new EventDB();
  private final SignupDB signupDB = new SignupDB();
  private final TournamentManager tournamentManager = new TournamentManager();

  @Override
  public void init() {
    route("GET", "/events").to("events.html").data(eventsData);
    route("GET", "/events/*").to("event.html").data(eventData);
    route("POST", "/events/create").to(createEvent);
    route("POST", "/events/*/setStatus").to(setStatus);
    route("POST", "/events/*/kick/*").to(kick);
  }

  private final Handler createEvent = (request, repsonse) -> {
    User user = request.get("user");
    checkState(user.isAdmin);

    Log.info(user + " is creating an event called " + request.param("name"));

    String datetime = request.param("date") + "T" + request.param("time");
    LocalDateTime date = LocalDateTime.parse(datetime);

    int timezoneOffset = parseInt(request.param("timezoneOffset"));
    date = date.plusMinutes(timezoneOffset);

    eventDB.createEvent(request.param("name"), date, request.param("prize"),
        request.param("format"), request.param("format_desc"), false);
  };

  private final Handler setStatus = (request, response) -> {
    User user = request.get("user");
    checkState(user.isAdmin);

    Status status = parseEnum(request.param("status"), Status.class);
    int eventId = request.getInt(1);

    JaxDB.db.transaction(() -> {
      eventDB.updateStatus(eventId, status);
      if (status == Status.IN_PROGRESS) {
        Event event = eventDB.get(eventId);
        tournamentManager.startTournament(event);
      }
    });
  };

  private final Handler kick = (request, response) -> {
    User user = request.get("user");
    checkState(user.isAdmin);

    int eventId = request.getInt(1);
    int battleId = request.getInt(3);

    signupDB.unregister(battleId, eventId);
  };

  private final Data eventsData = context -> {
    context.request.getPath();
    List<Event> events = eventDB.getAll();
    List<Event> pastEvents = Lists.newArrayList();
    for (int i = 0; i < events.size(); i++) {
      if (events.get(i).is(Status.COMPLETE)) {
        pastEvents.add(events.remove(i--));
      }
    }
    context.put("events", events);
    context.put("pastEvents", pastEvents);
  };

  private final Data eventData = context -> {
    Event event = eventDB.get(context.request.getInt(1));
    context.put("event", event);
    
    User user = context.get("user");

    boolean registered = user != null && signupDB.isRegistered(user.battleId, event.id);
    context.put("show-join", event.canJoin() && !registered);
    context.put("show-leave", event.canLeave() && registered);
    context.put("show-closed", event.is(Status.REGISTRATION_CLOSED) && !registered);
    context.put("started", event.is(Status.IN_PROGRESS, Status.COMPLETE));

    List<Integer> battleIds = signupDB.getPlayersSignedUpFor(event.id);
    List<User> users = userDB.getUsers(battleIds);

    context.put("players", users);

    context.put("statuses", Event.Status.values());

    context.put("rounds", tournamentManager.getRoundData(event.id));
  };

}
