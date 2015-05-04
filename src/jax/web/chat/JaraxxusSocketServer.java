package jax.web.chat;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Integer.parseInt;
import jasonlib.Json;
import jasonlib.Log;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jax.db.EventDB;
import jax.db.MatchDB;
import jax.db.SessionDB;
import jax.db.SignupDB;
import jax.db.UserDB;
import jax.model.Event;
import jax.model.Event.Status;
import jax.model.Match;
import jax.model.Player;
import jax.model.TournamentManager;
import jax.model.User;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

public class JaraxxusSocketServer extends WebSocketServer {

  private static final int CHAT_HISTORY_CAP = 50;

  public static JaraxxusSocketServer instance;

  private final Map<Integer, Player> idPlayers = Maps.newConcurrentMap();
  private final Map<WebSocket, Player> socketPlayers = Maps.newConcurrentMap();
  private final Multimap<String, Player> roomPlayers = LinkedListMultimap.create();
  private final LinkedListMultimap<String, Message> roomMessages = LinkedListMultimap.create();

  // maps eventID to battleTags of players who are ready for the event.
  public final Multimap<Long, String> readyPlayers = Multimaps.synchronizedMultimap(HashMultimap.create());

  private final SessionDB sessionDB = new SessionDB();
  private final UserDB userDB = new UserDB();
  private final EventDB eventDB = new EventDB();
  private final SignupDB signupDB = new SignupDB();
  private final MatchDB matchDB = new MatchDB();

  public final MessageFilter messageFilter = new MessageFilter(this);

  public JaraxxusSocketServer(int port) {
    super(new InetSocketAddress(port));

    JaraxxusSocketServer.instance = this;
  }

  public Player getPlayer(int chatId) {
    return idPlayers.get(chatId);
  }

  public void pairingsAreUp(long eventId, int round, List<Match> matches) {
    Event event = eventDB.get(eventId);

    Map<String, Match> playerMatches = Maps.newHashMap();
    for (Match match : matches) {
      playerMatches.put(match.playerA, match);
      playerMatches.put(match.playerB, match);
    }

    for (Player player : ImmutableList.copyOf(socketPlayers.values())) {
      Json json = Json.object().with("command", "next-round").with("event", eventId).with("round", round);
      if (player.user != null) {
        Match match = playerMatches.get(player.user.battleTag);
        if (match != null) {
          json.with("match", match.id);
          if (match.playerA.equals(player.user.battleTag)) {
            json.with("opponent", match.playerB);
          } else {
            json.with("opponent", match.playerA);
          }
        }
      }
      player.send(json);
    }

    String s = "<b>" + event.name + ": </b>" + "<a href='/events/" + eventId + "'>Pairings for round " + round
        + " are up.</a>";
    systemMessage(s);
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    Player player = new Player(conn);
    socketPlayers.put(conn, player);
    idPlayers.put(player.id, player);
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    Player player = socketPlayers.get(conn);

    Json json = new Json(message);

    String command = json.get("command");
    if (command.equals("login")) {
      checkState(player.user == null && player.guestName == null, "Player has already logged in!");

      String token = json.get("token");

      if (token == null) {
        player.guestName = "Guest";
      } else {
        player.user = userDB.get(sessionDB.getBattleId(token));

        if (player.user.isAdmin) {
          player.send(Json.object().with("command", "admin"));
        }
      }

      player.room = "/";
      synchronized (roomPlayers) {
        roomPlayers.put(player.room, player);
      }

      onEnter(player.room, player);
    } else if (command.equals("chat")) {
      Log.debug(player.getIP() + ": " + player.getName() + ": " + json.get("text"));
      String text = json.get("text");
      if (player.isAdmin() && text.startsWith("/")) {
        handleAdminCommand(text);
      } else {
        json.with("id", player.id);
        json.with("name", player.getName());
        Message m = new Message(player.getName(), player.id, text, player.isAdmin());
        logMessage(player.room, m);
        sendToRoom(player.room, m);
      }
    } else if (command.equals("issue-ready-check")) {
      checkState(player.user.isAdmin);
      Log.info(player.getName() + " has issued a ready-check");
      readyCheck(json.getInt("event"), player.getName());
    } else if (command.equals("ready")) {
      updateStatus(player);
      // sendToRoom(Json.object().with("command", "ready").with("player", player.getName()), "/");
    } else if (command.equals("not-ready")) {
      sendToRoom(Json.object().with("command", "not-ready").with("player", player.getName()), "/");
    } else if (command.equals("announce")) {
      Log.info(player.getName() + " announces: " + json.get("text"));
      checkState(player.user.isAdmin);
      announce(json.get("text"));
    } else {
      Log.warn("Unknown command: " + command);
    }
  }

  private void handleAdminCommand(String s) {
    List<String> m = ImmutableList.copyOf(Splitter.on(" ").split(s));
    String command = m.get(0).substring(1);
    String text = Joiner.on(" ").join(m.subList(1, m.size()));
    if (command.equalsIgnoreCase("announce")) {
      systemMessage(text);
    } else if (command.equalsIgnoreCase("sound")) {
      Json json = Json.object()
          .with("command", "sound")
          .with("url", m.get(1));
      sendToRoom(json, "/");
    } else if (command.equalsIgnoreCase("force-round")) {
      new TournamentManager().moveToNextRound(parseInt(m.get(1)));
    }
    else {
      Log.warn("Unknown command: " + command);
    }
  }

  private void readyCheck(int eventId, String sender) {
    List<Integer> battleIds = signupDB.getPlayersSignedUpFor(eventId);
    Set<User> users = Sets.newHashSet(userDB.getUsers(battleIds));

    Json json = Json.object().with("command", "ready-check").with("event", eventId).with("sender", sender);
    for (Player player : ImmutableList.copyOf(socketPlayers.values())) {
      if (users.contains(player.user)) {
        player.send(json);
      }
    }
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    Player player = socketPlayers.remove(conn);

    if (player.room != null) {
      synchronized (roomPlayers) {
        roomPlayers.remove(player.room, player);
      }

      onLeave(player.room, player);
    }

    player.socket = null;// cleanup
  }

  private void onEnter(String room, Player player) {
    Json array = Json.array();
    for (Player p : roomPlayers.get(room)) {
      if (p == player) {
        continue;
      }
      array.add(Json.object()
          .with("command", "join")
          .with("id", p.id)
          .with("name", p.getName()));
    }

    for (Message m : ImmutableList.copyOf(roomMessages.get(room))) {
      if (messageFilter.passes(m, player)) {
        array.add(m.toJson());
      }
    }
    player.send(array);

    sendToRoom(Json.object().with("command", "join")
        .with("id", player.id)
        .with("name", player.getName()), room);

    updateStatus(player);
  }

  private void updateStatus(Player player) {
    if (player.user == null) {
      return;
    }
    List<Long> eventIds = signupDB.getEventsForPlayer(player.user.battleId);
    List<Event> events = eventDB.getEvents(eventIds);

    Event upcoming = null;
    Event current = null;
    for (Event event : events) {
      if (event.is(Status.COMPLETE)) {
        continue;
      }
      if (event.is(Status.IN_PROGRESS)) {
        current = event;
        break;
      }
      if (upcoming == null || upcoming.date.isAfter(event.date)) {
        upcoming = event;
      }
    }

    if (current != null) {
      Match match = matchDB.getLatestMatch(current.id, player.user.battleTag);
      if (match != null) {
        Json json = Json.object()
            .with("command", "match-info")
            .with("event", current.id)
            .with("round", match.round)
            .with("match", match.id);

        if (match.playerA.equals(player.user.battleTag)) {
          json.with("opponent", match.playerB);
        } else {
          json.with("opponent", match.playerA);
        }
        player.send(json);
      }
      return;
    }

    if (upcoming == null || upcoming.date.isAfter(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))) {
      return;
    }

    boolean ready = upcoming.date.isBefore(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(31));
    if (ready) {
      readyPlayers.put(upcoming.id, player.user.battleTag);
    }

    player.send(Json.object()
        .with("command", "upcoming_event")
        .with("id", upcoming.id)
        .with("name", upcoming.name)
        .with("date", upcoming.getDateString())
        .with("ready", ready)
        );
  }

  private void onLeave(String room, Player player) {
    Json j = Json.object()
        .with("command", "leave")
        .with("id", player.id);
    sendToRoom(j, room);
  }

  public void announce(String message) {
    systemMessage("<b>Announcement: </b>" + message);
  }

  public void systemMessage(String message) {
    logMessage("/", new Message(null, null, message, false));
    sendToRoom(Json.object().with("command", "sys-msg").with("text", message), "/");
  }

  private void logMessage(String room, Message message) {
    synchronized (roomMessages) {
      roomMessages.put(room, message);
      List<Message> messages = roomMessages.get(room);
      while (messages.size() > CHAT_HISTORY_CAP) {
        messages.remove(0);
      }
    }
  }

  private void sendToRoom(String room, Message m) {
    Json j = m.toJson();
    for (Player p : ImmutableList.copyOf(roomPlayers.get(room))) {
      if (messageFilter.passes(m, p)) {
        p.send(j);
      }
    }
  }

  private void sendToRoom(Json j, String room) {
    for (Player p : ImmutableList.copyOf(roomPlayers.get(room))) {
      p.send(j);
    }
  }

  @Override
  public void onError(WebSocket conn, Exception e) {
    e.printStackTrace();
  }

}
