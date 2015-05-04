package jax.web.chat;

import jasonlib.Json;

public class Message {

  public final String from, text;
  public final Integer fromId;
  public final boolean admin;

  public Message(String from, Integer fromId, String text, boolean admin) {
    this.from = from;
    this.fromId = fromId;
    this.text = text;
    this.admin = admin;
  }

  public Json toJson() {
    if (from == null) {
      return Json.object()
          .with("command", "sys-msg")
          .with("text", text);
    } else {
      return Json.object()
          .with("command", "chat")
          .with("name", from)
          .with("id", fromId)
          .with("text", text)
          .with("admin", admin);
    }
  }

}
