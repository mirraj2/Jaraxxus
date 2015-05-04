package jax.web.chat;

import static com.google.common.base.Preconditions.checkState;
import jax.db.BanDB;
import jax.model.Player;
import jax.model.User;
import bowser.Controller;
import bowser.Handler;
import bowser.template.Data;

public class ChatProfilePage extends Controller {

  private final BanDB banDB = new BanDB();

  @Override
  public void init() {
    route("GET", "/chatprofile/*").to("chatprofile.html").data(chatProfile);
    route("POST", "/banchat/*").to(banchat);
  }

  private final Handler banchat = (request, response) -> {
    User user = request.get("user");
    checkState(user.isAdmin);

    int chatId = request.getInt(1);
    Player player = JaraxxusSocketServer.instance.getPlayer(chatId);

    banDB.ban(player);

    JaraxxusSocketServer.instance.messageFilter.reloadBanList();
  };

  private Data chatProfile = context -> {
    User user = context.get("user");
    checkState(user.isAdmin);

    int chatId = context.request.getInt(1);
    Player player = JaraxxusSocketServer.instance.getPlayer(chatId);

    context.put("name", player.getName());
    context.put("ip", player.getIP());
    context.put("chatid", chatId);
  };

}
