package jax.web.chat;

import java.util.Set;
import jax.db.BanDB;
import jax.model.Player;

public class MessageFilter {

  private final BanDB banDB = new BanDB();

  private final JaraxxusSocketServer socketServer;
  private Set<String> bannedIPs;
  private Set<Integer> bannedIds;

  public MessageFilter(JaraxxusSocketServer socketServer) {
    this.socketServer = socketServer;
    reloadBanList();
  }

  // this filter tries to make it so that the trolls don't even know they are banned.
  public boolean passes(Message m, Player target) {
    if (m.from == null) {
      return true; // system messages pass
    }

    if (m.fromId == target.id) {
      return true;
    }

    Player player = socketServer.getPlayer(m.fromId);

    if (player.user == null) {
      if (bannedIPs.contains(player.ip)) {
        if (target.user == null && player.ip.equals(target.ip)) {
          return true;
        }
        return false;
      }
    } else {
      if (bannedIds.contains(player.user.battleId)) {
        if (target.user == null) {
          if (player.ip.equals(target.ip)) {
            return true;
          }
        } else if (target.user.battleId == player.user.battleId) {
          return true;
        }
        return false;
      }
    }

    return true;
  }

  public void reloadBanList() {
    bannedIPs = banDB.getBannedIPs();
    bannedIds = banDB.getBannedBattleIds();
  }

}
