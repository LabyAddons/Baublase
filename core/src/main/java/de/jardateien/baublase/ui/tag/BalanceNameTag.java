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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BalanceNameTag extends ComponentNameTag {

  private static final List<Component> NO_COMPONENTS = List.of();

  private final BaublaseConfiguration configuration;
  private final Map<UUID, CachedComponents> componentsByUniqueId = new ConcurrentHashMap<>();

  public BalanceNameTag(BaublaseConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public boolean isVisible() {
    return this.configuration.enabled().get() && this.configuration.balance().get() && super.isVisible();
  }

  @Override
  protected @NotNull List<Component> buildComponents(EntitySnapshot snapshot) {
    if (!this.configuration.enabled().get() || !this.configuration.balance().get()) {
      return NO_COMPONENTS;
    }

    if (!snapshot.has(BaublaseExtraKeys.BAUBLASE_USER)) {
      return NO_COMPONENTS;
    }

    BaublaseUserSnapshot baublaseUser = snapshot.get(BaublaseExtraKeys.BAUBLASE_USER);
    Balance balance = baublaseUser.balance();
    if (balance == null || balance.uuid() == null) {
      return NO_COMPONENTS;
    }

    double amount = balance.balance();
    if (!Double.isFinite(amount) || amount < 0) {
      return NO_COMPONENTS;
    }

    CachedComponents cached = this.componentsByUniqueId.get(balance.uuid());
    if (cached != null && Double.compare(cached.amount(), amount) == 0) {
      return cached.components();
    }

    List<Component> components = List.of(
        Component.translatable("baublase.settings.balance.tag",
            Component.text(
                FormatUtil.getFormat(
                    amount,
                    RoundingMode.HALF_DOWN
                )
            )
        )
    );

    this.componentsByUniqueId.put(balance.uuid(), new CachedComponents(amount, components));
    return components;
  }

  private record CachedComponents(double amount, List<Component> components) {
  }
}
