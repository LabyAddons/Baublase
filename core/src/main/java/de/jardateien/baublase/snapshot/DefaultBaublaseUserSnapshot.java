package de.jardateien.baublase.snapshot;

import de.jardateien.baublase.BaublaseAddon;
import de.jardateien.baublase.api.snapshot.BaublaseUserSnapshot;
import de.jardateien.baublase.utils.BaublaseHandler;
import net.baublase.publicapi.client.model.Balance;
import net.baublase.publicapi.client.model.BountySummary;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.laby3d.renderer.snapshot.AbstractLabySnapshot;
import net.labymod.api.laby3d.renderer.snapshot.Extras;
import java.util.UUID;

public class DefaultBaublaseUserSnapshot extends AbstractLabySnapshot implements BaublaseUserSnapshot {

  private final BaublaseHandler handler;
  private final UUID uuid;

  public DefaultBaublaseUserSnapshot(Player player, Extras extras, BaublaseAddon addon) {
    super(extras);

    this.handler = addon.getHandler();
    this.uuid = player.getUniqueId();
  }

  @Override
  public BountySummary bounty() {
    return this.handler.getBounty(this.uuid);
  }

  @Override
  public Balance balance() {
    return this.handler.getBalance(this.uuid);
  }
}
