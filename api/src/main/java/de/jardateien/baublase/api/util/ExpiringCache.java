package de.jardateien.baublase.api.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ExpiringCache<K, V> {

  private static final ScheduledExecutorService SCHEDULER =
      Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Baublase-Cache");
        thread.setDaemon(true);
        return thread;
      });

  private final ConcurrentHashMap<K, Entry<V>> store =
      new ConcurrentHashMap<>();

  private final long ttlMillis;
  private final ScheduledFuture<?> purgeTask;

  public ExpiringCache(long duration, TimeUnit unit) {
    if (duration <= 0) {
      throw new IllegalArgumentException("Cache duration must be greater than 0");
    }

    this.ttlMillis = unit.toMillis(duration);

    long purgeInterval = Math.clamp(this.ttlMillis / 3,
        1_000L, 10_000L);

    this.purgeTask = SCHEDULER.scheduleAtFixedRate(
        this::purge,
        purgeInterval,
        purgeInterval,
        TimeUnit.MILLISECONDS
    );
  }

  /**
   * Stores a value with the configured expiration time.
   */
  public void put(K key, V value) {
    if (key == null || value == null) {
      return;
    }

    long expiresAt = System.currentTimeMillis() + this.ttlMillis;

    this.store.put(
        key,
        new Entry<>(value, expiresAt)
    );
  }

  /**
   * Returns the cached value if it has not expired.
   */
  public V get(K key) {
    if (key == null) {
      return null;
    }

    Entry<V> entry = this.store.get(key);

    if (entry == null) {
      return null;
    }

    if (entry.isExpired()) {
      /*
       * Only remove the entry if it is still the same entry.
       *
       * This prevents an older cache entry from accidentally
       * removing a newer value that was inserted concurrently.
       */
      this.store.remove(key, entry);
      return null;
    }

    return entry.value();
  }

  /**
   * Removes all cached values.
   */
  public void clear() {
    this.store.clear();
  }

  /**
   * Returns the current number of cached entries.
   */
  public int size() {
    return this.store.size();
  }

  /**
   * Stops the purge task for this cache.
   */
  public void shutdown() {
    this.purgeTask.cancel(false);
    this.store.clear();
  }

  /**
   * Removes expired entries.
   */
  private void purge() {
    long now = System.currentTimeMillis();

    for (Map.Entry<K, Entry<V>> entry : this.store.entrySet()) {
      Entry<V> value = entry.getValue();

      if (value.expirationTime() <= now) {
        this.store.remove(entry.getKey(), value);
      }
    }
  }

  /**
   * Immutable cache entry.
   */
  public record Entry<V>(
      V value,
      long expirationTime
  ) {

    public boolean isExpired() {
      return System.currentTimeMillis() >= this.expirationTime;
    }
  }
}