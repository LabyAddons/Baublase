package de.jardateien.baublase.api.util;

import java.util.Map;
import java.util.concurrent.*;

public class ExpiringCache<K, V> {

  private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
      r -> {
        Thread thread = new Thread(r, "ExpiringCache-Scheduler");
        thread.setDaemon(true);
        return thread;
      }
  );

  private final ConcurrentHashMap<K, Entry<V>> store = new ConcurrentHashMap<>();
  private final long ttlMillis;
  private final ScheduledFuture<?> task;

  public ExpiringCache(long duration, TimeUnit unit) {
    this.ttlMillis = unit.toMillis(duration);
    long period = Math.max(1, this.ttlMillis / 3);
    this.task = scheduler.scheduleAtFixedRate(this::purge, period, period, unit);
  }

  public void put(K key, V value) {
    long expiresAt = System.currentTimeMillis() + this.ttlMillis;
    this.store.put(key, new Entry<>(value, expiresAt));
  }

  public V get(K key) {
    Entry<V> entry = this.store.get(key);
    if (entry == null) {
      return null;
    }

    if (entry.isExpired()) {
      this.store.remove(key);
      return null;
    }
    return entry.value;
  }

  public boolean containsKey(K key) {
    return get(key) != null;
  }

  public void remove(K key) {
    this.store.remove(key);
  }

  public void clear() {
    this.store.clear();
  }

  public void shutdown() {
    this.task.cancel(false);
  }

  private void purge() {
    for (Map.Entry<K, Entry<V>> entry : this.store.entrySet()) {
      if (entry.getValue().isExpired()) {
        this.store.remove(entry.getKey());
      }
    }
  }

  public static class Entry<V> {
    private final V value;
    private final long expirationTime;

    public Entry(V value, long expirationTime) {
      this.value = value;
      this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
      return System.currentTimeMillis() > this.expirationTime;
    }
  }
}
