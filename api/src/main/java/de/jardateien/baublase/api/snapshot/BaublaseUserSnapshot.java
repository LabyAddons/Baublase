package de.jardateien.baublase.api.snapshot;

import net.baublase.publicapi.client.model.Balance;
import net.baublase.publicapi.client.model.BountySummary;
import net.labymod.api.laby3d.renderer.snapshot.LabySnapshot;

public interface BaublaseUserSnapshot extends LabySnapshot {

  BountySummary bounty();
  Balance balance();

}
