package jax.upgrade;

import ez.DB;

public abstract class UpgradeStep {

  public abstract void upgrade(DB db);

}
