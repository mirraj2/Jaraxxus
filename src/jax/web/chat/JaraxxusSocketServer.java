package jax.web.chat;

import static com.google.common.base.Preconditions.checkState;
import jasonlib.Json;
import jasonlib.Log;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jax.db.EventDB;
import jax.db.SessionDB;
import jax.db.SignupDB;
import jax.db.UserDB;
import jax.model.Event;
import jax.model.Match;
import jax.model.Player;
import jax.model.User;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class JaraxxusSocketServer extends WebSocketServer {

  public static JaraxxusSocketServer instance;

  private final Map<WebSocket, Player> socketPlayers = Maps.newConcurrentMap();
  private final Multimap<String, Player> roomPlayers = LinkedListMultimap.create();
  private final Multimap<String, Message> roomMessages = LinkedListMultimap.create();

  private final SessionDB sessionDB = new SessionDB();
  private final UserDB userDB = new UserDB();
  private final EventDB eventDB = new EventDB();
  private final SignupDB signupDB = new SignupDB();

  public JaraxxusSocketServer(int port) {
    super(new InetSocketAddress(port));

    JaraxxusSocketServer.instance = this;
  }

  public void pairingsAreUp(long eventId, int round, List<Match> matches) {
    Event event = eventDB.get(eventId);

    Set<String> playersInRound = Sets.newHashSet();
    for (Match match : matches) {
      playersInRound.add(match.playerA);
      playersInRound.add(match.playerB);
    }

    for (Player player : ImmutableList.copyOf(socketPlayers.values())) {
      player.send(Json.object().with("command", "next-round").with("event", eventId).with("round", round)
          .with("playing", playersInRound.contains(player.getName())));
    }

    String s = "<b>" + event.name + ": </b>" + "<a href='/events/" + eventId + "'>Pairings for round " + round
        + " are up.</a>";
    systemMessage(s);
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    socketPlayers.put(conn, new Player(conn));
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
        json.with("name", player.getName());
        roomMessages.put(player.room, new Message(player.getName(), text));
        sendToRoom(json, player.room);
      }
    } else if (command.equals("issue-ready-check")) {
      checkState(player.user.isAdmin);
      Log.info(player.getName() + " has issued a ready-check");
      readyCheck(json.getInt("event"), player.getName());
    } else if (command.equals("ready")) {
      sendToRoom(Json.object().with("command", "ready").with("player", player.getName()), "/");
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
          .with("command","sound")
          .with("url", m.get(1));
      sendToRoom(json, "/");
    } else {
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
      if (m.from == null) {
        array.add(Json.object()
            .with("command", "sys-msg")
            .with("text", m.text));
      } else {
        array.add(Json.object()
            .with("command", "chat")
            .with("name", m.from)
            .with("text", m.text));
      }
    }
    player.send(array);

    sendToRoom(Json.object().with("command", "join")
        .with("id", player.id)
        .with("name", player.getName()), room);
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
    roomMessages.put("/", new Message(null, message));
    sendToRoom(Json.object().with("command", "sys-msg").with("text", message), "/");
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
