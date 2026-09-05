package de.jardateien.baublase;

import de.jardateien.baublase.config.BaublaseConfiguration;
import de.jardateien.baublase.listeners.AuthenticatorListener;
import de.jardateien.baublase.ui.tag.BalanceNameTag;
import de.jardateien.baublase.ui.tag.BountyNameTag;
import de.jardateien.baublase.utils.BaublaseHandler;
import net.labymod.api.Laby;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.entity.player.tag.PositionType;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class BaublaseAddon extends LabyAddon<BaublaseConfiguration> {

  private final BaublaseHandler handler = new BaublaseHandler();
  private AuthenticatorListener authenticatorListener;

  @Override
  protected void enable() {
    this.authenticatorListener = new AuthenticatorListener(this);
    this.registerListener(this.authenticatorListener);

    Laby.references().tagRegistry().register(
        "baublase_bounty",
        PositionType.BELOW_NAME,
        new BountyNameTag(this.configuration())
    );

    Laby.references().tagRegistry().register(
        "baublase_balance",
        PositionType.ABOVE_NAME,
        new BalanceNameTag(this.configuration())
    );

    this.registerSettingCategory();
  }

  @Override
  protected void onDeactivated() {
    if (this.authenticatorListener != null) {
      this.authenticatorListener.deactivate();
    }
  }

  @Override
  protected void onActivated() {
    if (this.authenticatorListener != null) {
      this.authenticatorListener.refreshCurrentServer();
    }
  }

  public BaublaseHandler getHandler() {
    return this.handler;
  }

  @Override
  protected Class<BaublaseConfiguration> configurationClass() {
    return BaublaseConfiguration.class;
  }
}
