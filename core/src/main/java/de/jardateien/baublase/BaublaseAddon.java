package de.jardateien.baublase;

import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class BaublaseAddon extends LabyAddon<BaublaseConfiguration> {

  @Override
  protected void enable() {
    this.registerSettingCategory();
  }

  @Override
  protected Class<BaublaseConfiguration> configurationClass() {
    return BaublaseConfiguration.class;
  }
}
