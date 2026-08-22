package de.jardateien.baublase.listeners;

import de.jardateien.baublase.BaublaseAddon;
import de.jardateien.baublase.config.BaublaseConfiguration;
import de.jardateien.baublase.utils.BaublaseHandler;
import de.jardateien.baublase.utils.FormatUtil;
import net.baublase.publicapi.client.model.ItemPrice;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.session.Session;
import net.labymod.api.client.world.item.ItemStack;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.session.SessionUpdateEvent;
import net.labymod.api.event.client.world.ItemStackTooltipEvent;
import java.math.RoundingMode;
import java.util.List;

public class AuthenticatorListener {

  private final BaublaseHandler handler;
  private final BaublaseConfiguration configuration;

  public AuthenticatorListener(BaublaseAddon baublaseAddon) {
    this.handler = baublaseAddon.getHandler();
    this.configuration = baublaseAddon.configuration();
  }

  @Subscribe
  public void onSessionUpdate(SessionUpdateEvent updateEvent) {
    Session session = updateEvent.newSession();
    this.handler.authenticate(session.getUsername(), session.getUniqueId().toString(), session.getAccessToken());
    this.handler.loadItemPrices();
  }

  @Subscribe
  public void onServerDisconnect(ServerDisconnectEvent disconnectEvent) {
    this.handler.clearCache();
  }

  @Subscribe
  public void onItemTip(ItemStackTooltipEvent tooltipEvent) {
    if(!this.configuration.worth().get())
      return;

    ItemStack itemStack = tooltipEvent.itemStack();
    ResourceLocation identifier = itemStack.getAsItem().getIdentifier();
    List<Component> tooltipLines = tooltipEvent.getTooltipLines();
    if(identifier.getNamespace().equals("minecraft")) {
      ItemPrice itemPrices = this.handler.getItemPrices(identifier.getNamespace() + ":" + identifier.getPath());
      if(itemPrices == null)
        return;

      tooltipLines.add(Component.empty());
      tooltipLines.add(
          Component.translatable(
              "baublase.settings.worth.price.buy",
              TextColor.color(170, 170, 170),
              Component.text(
                  FormatUtil.getFormat(
                      itemPrices.buyPrice(),
                      RoundingMode.HALF_DOWN
                  ),
                  TextColor.color(255,255,85)
              )
          )
      );

      tooltipLines.add(
          Component.translatable(
              "baublase.settings.worth.price.sell",
              TextColor.color(170, 170, 170),
              Component.text(
                  FormatUtil.getFormat(
                      itemPrices.sellPrice() * itemStack.getSize(),
                      RoundingMode.HALF_DOWN
                  ),
                  TextColor.color(255,255,85)
              )
          )
      );
    }
  }

}
