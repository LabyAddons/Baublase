package de.jardateien.baublase.config;

import net.labymod.api.Laby;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.annotation.IntroducedIn;
import net.labymod.api.configuration.loader.annotation.SpriteSlot;
import net.labymod.api.configuration.loader.annotation.SpriteTexture;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.annotation.SettingSection;
import net.labymod.api.util.MethodOrder;

@SpriteTexture("settings.png")
@ConfigName("settings")
public class BaublaseConfiguration extends AddonConfig {

  @SettingSection(value = "general")
  @IntroducedIn(namespace = "baublase", value = "1.0.0")
  @SpriteSlot(size = 32)
  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  @SpriteSlot(x = 1, size = 32)
  @IntroducedIn(namespace = "baublase", value = "1.0.0")
  @MethodOrder(after = "enabled")
  @ButtonSetting
  private void discord() {
    Laby.references().chatExecutor().openUrl("https://discord.gg/baublase");
  }

  @SettingSection(value = "settings")
  @IntroducedIn(namespace = "baublase", value = "1.0.0")
  @SpriteSlot(y = 1, size = 32)
  @SwitchSetting
  private final ConfigProperty<Boolean> bounty = new ConfigProperty<>(true);
  @IntroducedIn(namespace = "baublase", value = "1.0.0")
  @SpriteSlot(x = 2, size = 32)
  @SwitchSetting
  private final ConfigProperty<Boolean> balance = new ConfigProperty<>(false);

  @IntroducedIn(namespace = "baublase", value = "1.0.0")
  @SpriteSlot(x = 3, size = 32)
  @SwitchSetting
  private final ConfigProperty<Boolean> worth = new ConfigProperty<>(false);

  //GETTER

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<Boolean> bounty() {
    return this.bounty;
  }
  public ConfigProperty<Boolean> worth() {
    return this.worth;
  }
  public ConfigProperty<Boolean> balance() {
    return this.balance;
  }
}
