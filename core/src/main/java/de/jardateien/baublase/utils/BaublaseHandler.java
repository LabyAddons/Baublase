package de.jardateien.baublase.utils;

import de.jardateien.baublase.api.util.ExpiringCache;
import net.baublase.publicapi.client.BaublaseApiClient;
import net.baublase.publicapi.client.BaublaseApiException;
import net.baublase.publicapi.client.model.Balance;
import net.baublase.publicapi.client.model.BountySummary;
import net.baublase.publicapi.client.model.ItemPrice;
import net.labymod.api.labyconnect.TokenStorage;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BaublaseHandler {

  private static final long BALANCE_CACHE_TIME = TimeUnit.SECONDS.toMillis(20);
  private static final long BOUNTY_CACHE_TIME = TimeUnit.SECONDS.toMillis(20);

  private final BaublaseApiClient client = BaublaseApiClient.builder()
      .baseUrl("https://api.baublase.net")
      .connectTimeout(Duration.ofSeconds(5))
      .requestTimeout(Duration.ofSeconds(10))
      .build();

  private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "Baublase-API");
    thread.setDaemon(true);
    return thread;
  });

  private final Map<UUID, AtomicBoolean> balanceRequests = new ConcurrentHashMap<>();
  private final AtomicBoolean bountyRequestRunning = new AtomicBoolean(false);
  private final AtomicBoolean authenticationRunning = new AtomicBoolean(false);
  private final AtomicBoolean itemPricesRequestRunning = new AtomicBoolean(false);

  private final ExpiringCache<UUID, Balance> balances =
      new ExpiringCache<>(BALANCE_CACHE_TIME, TimeUnit.MILLISECONDS);

  private final Map<UUID, BountySummary> bounties =
      new ConcurrentHashMap<>();

  private volatile Map<String, ItemPrice> itemPrices = Map.of();
  private volatile long lastBountyFetch = 0L;
  private volatile boolean baublaseServer = false;
  private volatile boolean authenticated = false;
  private volatile UUID authenticatedUniqueId;
  private volatile long tokenExpiresAt = 0L;

  public void setBaublaseServer(boolean baublaseServer) {
    this.baublaseServer = baublaseServer;

    if (!baublaseServer) {
      this.clearSession();
      this.clearCache();
    }
  }

  public boolean isBaublaseServer() {
    return this.baublaseServer;
  }

  public void authenticate(UUID uniqueId, TokenStorage tokenStorage) {
    if (!this.baublaseServer || uniqueId == null || tokenStorage == null) {
      return;
    }

    if (this.hasValidApiToken(uniqueId)) {
      this.loadItemPrices();
      return;
    }

    if (!this.authenticationRunning.compareAndSet(false, true)) {
      return;
    }

    this.executor.execute(() -> {
      try {
        if (!this.baublaseServer) {
          return;
        }

        TokenStorage.Token token =
            tokenStorage.getToken(TokenStorage.Purpose.JWT, uniqueId);

        if (token == null || !token.isValid()) {
          this.clearSession();
          return;
        }

        this.client.useToken(token.getToken());
        this.authenticated = true;
        this.authenticatedUniqueId = uniqueId;
        this.tokenExpiresAt = token.getExpiresAt();
        this.loadItemPrices();
      } catch (Exception exception) {
        this.clearSession();
      } finally {
        this.authenticationRunning.set(false);
      }
    });
  }

  public Balance getBalance(UUID uuid) {
    if (!this.isApiAvailable() || uuid == null) {
      return null;
    }

    Balance cached = this.balances.get(uuid);

    if (cached != null) {
      return cached;
    }

    this.requestBalance(uuid);
    return null;
  }

  private void requestBalance(UUID uuid) {
    AtomicBoolean requestRunning = this.balanceRequests.computeIfAbsent(
        uuid, ignored -> new AtomicBoolean(false)
    );

    if (!requestRunning.compareAndSet(false, true)) {
      return;
    }

    this.executor.execute(() -> {
      try {
        if (!this.isApiAvailable()) {
          return;
        }

        Balance balance = this.client.balance(uuid);

        if (balance != null) {
          this.balances.put(uuid, balance);
        }
      } catch (BaublaseApiException exception) {
        this.balances.put(uuid, new Balance(uuid, -1D));
      } finally {
        requestRunning.set(false);
      }
    });
  }

  public BountySummary getBounty(UUID uuid) {
    if (!this.isApiAvailable() || uuid == null) {
      return null;
    }

    this.updateBounties();
    return this.bounties.get(uuid);
  }

  private void updateBounties() {
    if (!this.isApiAvailable()) {
      return;
    }

    long now = System.currentTimeMillis();
    if (now - this.lastBountyFetch < BOUNTY_CACHE_TIME) {
      return;
    }

    this.lastBountyFetch = now;

    if (!this.bountyRequestRunning.compareAndSet(false, true)) {
      return;
    }

    this.executor.execute(() -> {
      try {
        if (!this.isApiAvailable()) {
          return;
        }

        List<BountySummary> fetchedBounties = this.client.bounties();

        if (fetchedBounties == null) {
          return;
        }

        Map<UUID, BountySummary> newBounties = new HashMap<>();

        for (BountySummary bounty : fetchedBounties) {
          if (bounty != null && bounty.uuid() != null) {
            newBounties.put(bounty.uuid(), bounty);
          }
        }

        this.bounties.clear();
        this.bounties.putAll(newBounties);
      } catch (BaublaseApiException exception) {
        // Keep existing bounty data until the next throttled refresh attempt.
      } finally {
        this.bountyRequestRunning.set(false);
      }
    });
  }

  public ItemPrice getItemPrices(String itemIdentifier) {
    if (!this.isApiAvailable() || itemIdentifier == null) {
      return null;
    }

    return this.itemPrices.get(itemIdentifier);
  }

  public void loadItemPrices() {
    if (!this.isApiAvailable()) {
      return;
    }

    if (!this.itemPrices.isEmpty()) {
      return;
    }

    if (!this.itemPricesRequestRunning.compareAndSet(false, true)) {
      return;
    }

    this.executor.execute(() -> {
      try {
        if (!this.isApiAvailable()) {
          return;
        }

        List<ItemPrice> prices = this.client.itemPrices();

        if (prices == null) {
          return;
        }

        Map<String, ItemPrice> pricesByIdentifier = new HashMap<>();
        for (ItemPrice price : prices) {
          if (price != null && price.itemIdentifier() != null) {
            pricesByIdentifier.put(price.itemIdentifier(), price);
          }
        }

        this.itemPrices = Map.copyOf(pricesByIdentifier);
      } catch (BaublaseApiException exception) {
        // Keep current cache.
      } finally {
        this.itemPricesRequestRunning.set(false);
      }
    });
  }

  public void clearCache() {
    this.balances.clear();
    this.bounties.clear();
    this.itemPrices = Map.of();
    this.lastBountyFetch = 0L;
    this.balanceRequests.clear();
  }

  private boolean isApiAvailable() {
    return this.baublaseServer
        && this.authenticated
        && this.client.currentToken() != null
        && this.tokenExpiresAt > System.currentTimeMillis();
  }

  private boolean hasValidApiToken(UUID uniqueId) {
    return this.isApiAvailable()
        && uniqueId.equals(this.authenticatedUniqueId);
  }

  private void clearSession() {
    this.authenticated = false;
    this.authenticatedUniqueId = null;
    this.tokenExpiresAt = 0L;
  }
}
