package jax.upgrade;

import jasonlib.Log;
import java.util.List;
import jax.db.JaxDB;
import jax.db.VersionDB;
import com.google.common.collect.Lists;

public class Backcompat {

  private final VersionDB versionDB = new VersionDB();
  private List<UpgradeStep> steps = Lists.newArrayList();

  public Backcompat() {
    // steps.add(new Step1());
  }

  public void run() {
    int version = versionDB.getCurrentVersion();
    while (version < steps.size()) {
      Log.info("Upgrading from version " + version + " to version " + (version + 1));
      UpgradeStep step = steps.get(version++);
      JaxDB.db.transaction(() -> step.upgrade(JaxDB.db));
      versionDB.incVersion();
    }
  }

}
