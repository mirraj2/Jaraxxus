package jax.model;

public class User {

  public final int battleId;
  public final String battleTag;
  public final String name;
  public final boolean isAdmin;

  public User(int battleId, String battleTag, boolean isAdmin) {
    this.battleId = battleId;
    this.battleTag = battleTag;
    this.isAdmin = isAdmin;

    this.name = prettify(battleTag);
  }

  @Override
  public String toString() {
    return battleTag;
  }

  public static String prettify(String battleTag) {
    if (battleTag.indexOf("#") == -1) {
      return battleTag;
    }
    return Character.toUpperCase(battleTag.charAt(0)) + battleTag.substring(1, battleTag.indexOf("#"));
  }

  @Override
  public int hashCode() {
    return battleId;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof User)) {
      return false;
    }
    User that = (User) obj;
    return this.battleId == that.battleId;
  }

}
