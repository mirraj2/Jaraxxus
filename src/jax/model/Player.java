package jax.model;

import jasonlib.Json;
import java.util.concurrent.atomic.AtomicInteger;
import org.java_websocket.WebSocket;
import com.google.common.collect.ImmutableList;

public class Player {

  private static AtomicInteger counter = new AtomicInteger();

  private final WebSocket socket;

  public final int id = counter.getAndIncrement();
  public User user;
  public String guestName;
  public String room;

  public Player(WebSocket socket) {
    this.socket = socket;
  }

  public String getIP() {
    return socket.getRemoteSocketAddress().getHostString();
  }

  public void send(Json json) {
    if (json.isArray()) {
      if (json.isEmpty()) {
        return;
      }
    } else {
      json = Json.array(ImmutableList.of(json));
    }
    try {
      socket.send(json.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getName() {
    return user != null ? user.name : guestName;
  }

  public boolean isAdmin() {
    return user != null && user.isAdmin;
  }

}
