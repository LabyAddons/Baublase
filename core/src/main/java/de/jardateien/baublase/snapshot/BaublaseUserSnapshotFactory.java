package de.jardateien.baublase.snapshot;

import de.jardateien.baublase.BaublaseAddon;
import de.jardateien.baublase.api.snapshot.BaublaseExtraKeys;
import de.jardateien.baublase.api.snapshot.BaublaseUserSnapshot;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.laby3d.renderer.snapshot.Extras;
import net.labymod.api.laby3d.renderer.snapshot.LabySnapshotFactory;
import net.labymod.api.service.annotation.AutoService;

@AutoService(LabySnapshotFactory.class)
public class BaublaseUserSnapshotFactory extends LabySnapshotFactory<Player, BaublaseUserSnapshot> {

  private final BaublaseAddon addon;

  public BaublaseUserSnapshotFactory(BaublaseAddon addon) {
    super(BaublaseExtraKeys.BAUBLASE_USER);
    this.addon = addon;
  }

  @Override
  protected BaublaseUserSnapshot create(Player player, Extras extras) {
    return new DefaultBaublaseUserSnapshot(player, extras, this.addon);
  }



}