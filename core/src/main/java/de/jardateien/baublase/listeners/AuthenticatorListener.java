package de.jardateien.baublase.listeners;

import de.jardateien.baublase.BaublaseAddon;
import de.jardateien.baublase.config.BaublaseConfiguration;
import de.jardateien.baublase.utils.BaublaseHandler;
import de.jardateien.baublase.utils.FormatUtil;
import net.baublase.publicapi.client.model.ItemPrice;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.network.server.ServerAddress;
import net.labymod.api.client.network.server.ServerData;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.session.Session;
import net.labymod.api.client.world.item.ItemStack;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;
import net.labymod.api.event.client.network.server.ServerSwitchEvent;
import net.labymod.api.event.client.session.SessionUpdateEvent;
import net.labymod.api.event.client.world.ItemStackTooltipEvent;
import net.labymod.api.labyconnect.TokenStorage;

import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AuthenticatorListener {

  private static final String BAUBLASE_HOST = "baublase.net";

  private final BaublaseHandler handler;
  private final BaublaseConfiguration configuration;
  private final TokenStorage tokenStorage;

  public AuthenticatorListener(BaublaseAddon baublaseAddon) {
    this.handler = baublaseAddon.getHandler();
    this.configuration = baublaseAddon.configuration();
    this.tokenStorage = Laby.references().tokenStorage();
  }

  @Subscribe
  public void onSessionUpdate(SessionUpdateEvent updateEvent) {
    Session session = updateEvent.newSession();
    if (session == null) {
      return;
    }

    this.tryAuthenticate(session.getUniqueId());
  }

  @Subscribe
  public void onServerConnect(ServerJoinEvent joinEvent) {
    this.updateServerState(joinEvent.serverData());
  }

  @Subscribe
  public void onServerSwitch(ServerSwitchEvent switchEvent) {
    this.updateServerState(switchEvent.newServerData());
  }

  @Subscribe
  public void onServerDisconnect(ServerDisconnectEvent disconnectEvent) {
    this.handler.setBaublaseServer(false);
  }

  public void deactivate() {
    this.handler.setBaublaseServer(false);
  }

  public void refreshCurrentServer() {
    this.updateServerState(Laby.labyAPI().serverController().getCurrentServerData());
  }

  @Subscribe
  public void onItemTip(ItemStackTooltipEvent tooltipEvent) {
    if (!this.configuration.enabled().get()) {
      return;
    }

    if (!this.configuration.worth().get()) {
      return;
    }

    if (!this.handler.isBaublaseServer()) {
      return;
    }

    ItemStack itemStack = tooltipEvent.itemStack();
    itemStack.getAsItem();

    ResourceLocation identifier = itemStack.getAsItem().getIdentifier();

    if (identifier == null) {
      return;
    }

    if (!identifier.getNamespace().equals("minecraft")) {
      return;
    }

    String itemIdentifier =
        identifier.getNamespace() + ":" + identifier.getPath();

    ItemPrice itemPrice =
        this.handler.getItemPrices(itemIdentifier);

    if (itemPrice == null) {
      return;
    }

    List<Component> tooltipLines = tooltipEvent.getTooltipLines();

    tooltipLines.add(Component.empty());

    tooltipLines.add(
        Component.translatable(
            "baublase.settings.worth.price.buy",
            TextColor.color(170, 170, 170),
            Component.text(
                FormatUtil.getFormat(
                    itemPrice.buyPrice(),
                    RoundingMode.HALF_DOWN
                ),
                TextColor.color(255, 255, 85)
            )
        )
    );

    tooltipLines.add(
        Component.translatable(
            "baublase.settings.worth.price.sell",
            TextColor.color(170, 170, 170),
            Component.text(
                FormatUtil.getFormat(
                    itemPrice.sellPrice() * itemStack.getSize(),
                    RoundingMode.HALF_DOWN
                ),
                TextColor.color(255, 255, 85)
            )
        )
    );
  }

  private void updateServerState(ServerData serverData) {
    boolean baublaseServer =
        this.configuration.enabled().get() && isBaublaseServer(serverData);

    this.handler.setBaublaseServer(baublaseServer);

    if (!baublaseServer) {
      return;
    }

    Session session = Laby.labyAPI().minecraft().sessionAccessor().getSession();
    if (session != null) {
      this.tryAuthenticate(session.getUniqueId());
    }
  }

  private void tryAuthenticate(UUID uniqueId) {
    if (!this.configuration.enabled().get()) {
      this.handler.setBaublaseServer(false);
      return;
    }

    if (!this.handler.isBaublaseServer()) {
      return;
    }

    this.handler.authenticate(uniqueId, this.tokenStorage);
  }

  private static boolean isBaublaseServer(ServerData serverData) {
    if (serverData == null) {
      return false;
    }

    ServerAddress address = serverData.address();
    if (address == null || address.getHost() == null) {
      return false;
    }

    String host = address.getHost().toLowerCase(Locale.ROOT);
    return host.equals(BAUBLASE_HOST) || host.endsWith("." + BAUBLASE_HOST);
  }
}
