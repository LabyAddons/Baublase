package de.jardateien.baublase.ui.tag;

import de.jardateien.baublase.api.snapshot.BaublaseExtraKeys;
import de.jardateien.baublase.api.snapshot.BaublaseUserSnapshot;
import de.jardateien.baublase.config.BaublaseConfiguration;
import de.jardateien.baublase.utils.FormatUtil;
import net.baublase.publicapi.client.model.Balance;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.tag.tags.ComponentNameTag;
import net.labymod.api.client.render.state.entity.EntitySnapshot;
import org.jetbrains.annotations.NotNull;
import java.math.RoundingMode;
import java.util.List;

public class BalanceNameTag extends ComponentNameTag {

  private final BaublaseConfiguration configuration;

  public BalanceNameTag(BaublaseConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public boolean isVisible() {
    return this.configuration.enabled().get() && this.configuration.balance().get() && super.isVisible();
  }

  @Override
  protected @NotNull List<Component> buildComponents(EntitySnapshot snapshot) {
    if(!this.configuration.bounty().get())
      return super.buildComponents(snapshot);

    if(!snapshot.has(BaublaseExtraKeys.BAUBLASE_USER))
      return super.buildComponents(snapshot);

    BaublaseUserSnapshot baublaseUser = snapshot.get(BaublaseExtraKeys.BAUBLASE_USER);
    Balance balance = baublaseUser.balance();
    if(balance == null || balance.balance() < 0)
      return super.buildComponents(snapshot);

    return List.of(
        Component.translatable("baublase.settings.balance.tag",
            Component.text(
                FormatUtil.getFormat(
                    balance.balance(),
                    RoundingMode.HALF_DOWN
                )
            )
        )
    );
  }


}
