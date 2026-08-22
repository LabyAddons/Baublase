package de.jardateien.baublase.utils;

import de.jardateien.baublase.api.util.ExpiringCache;
import net.baublase.publicapi.client.BaublaseApiClient;
import net.baublase.publicapi.client.BaublaseApiException;
import net.baublase.publicapi.client.auth.MinecraftCredentials;
import net.baublase.publicapi.client.model.Balance;
import net.baublase.publicapi.client.model.BountySummary;
import net.baublase.publicapi.client.model.ItemPrice;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BaublaseHandler {

  private final BaublaseApiClient client = BaublaseApiClient.builder().baseUrl("https://api.baublase.net").build();

  private final ExpiringCache<UUID, Balance> balances = new ExpiringCache<>(20, TimeUnit.SECONDS);
  private final Map<UUID, BountySummary> bounties = new ConcurrentHashMap<>();
  private List<ItemPrice> itemPrices = new ArrayList<>();
  private long lastFetch = 0;

  public void authenticate(String username, String uuid, String accessToken) {
    try {
      this.client.authenticate(new MinecraftCredentials(username, uuid, accessToken));
    } catch (BaublaseApiException ignore) {}
  }

  public Balance getBalance(UUID uuid) {
    if(this.client.currentToken() == null)
      return null;

    if(this.balances.containsKey(uuid)) {
      return this.balances.get(uuid);
    }

    try {
      Balance balance = this.client.balance(uuid);
      this.balances.put(uuid, balance);
      return balance;
    } catch (BaublaseApiException exception) {
      Balance balance = new Balance(uuid, -1);
      this.balances.put(uuid, balance);
      return balance;
    }
  }

  public BountySummary getBounty(UUID uuid) {
    updateBounties();
    return this.bounties.get(uuid);
  }

  private void updateBounties() {
    if(this.client.currentToken() == null)
      return;

    if(System.currentTimeMillis() - this.lastFetch < 20000) {
      return;
    }

    List<BountySummary> bounties = this.client.bounties();
    this.bounties.clear();
    for (BountySummary bounty : bounties) {
      this.bounties.put(bounty.uuid(), bounty);
    }

    this.lastFetch = System.currentTimeMillis();
  }

  public ItemPrice getItemPrices(String itemIdentifier) {
    for (ItemPrice itemPrice : this.itemPrices) {
      if(itemPrice.itemIdentifier().equals(itemIdentifier))
        return itemPrice;
    }

    return null;
  }

  public void clearCache() {
    this.balances.clear();
    this.bounties.clear();
  }

  public void loadItemPrices() {
    if(this.client.currentToken() == null)
      return;

    if(!this.itemPrices.isEmpty())
      return;

    this.itemPrices = this.client.itemPrices();
  }
}
