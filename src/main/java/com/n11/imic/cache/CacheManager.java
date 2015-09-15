package com.n11.imic.cache;

import com.n11.imic.config.ImageScalerConfigFileLocator;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.apache.commons.configuration.XMLConfiguration;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CacheManager {

    private Cache imageCache;
    private net.sf.ehcache.CacheManager cacheManager = null;
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private CacheManager() {
    }

    public void initConfiguration() {
        try {
            shutdownCacheIfNeeded();

            XMLConfiguration xmlConfiguration = new XMLConfiguration(ImageScalerConfigFileLocator.findConfigFile());
            if (xmlConfiguration.getKeys("cache").hasNext()) {
                CacheConfiguration cacheConfiguration = initCacheConfiguration(xmlConfiguration);
                initCacheManager(cacheConfiguration, xmlConfiguration.getString("cache[@diskStorePath]", ""));
            }
        } catch (Exception ce) {
            cacheLock.writeLock().unlock();
            throw new IllegalArgumentException("Cannot instantiate cache, problem with the configuration", ce);
        }
    }

    private CacheConfiguration initCacheConfiguration(XMLConfiguration xmlConfiguration) {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName("ImageCache");
        cacheConfiguration.setEternal(xmlConfiguration.getBoolean("cache[@eternal]", false));
        cacheConfiguration.setMaxEntriesLocalHeap(xmlConfiguration.getLong("cache[@maxEntriesLocalHeap]", CacheConfiguration.DEFAULT_MAX_ENTRIES_IN_CACHE));
        cacheConfiguration.setMaxEntriesInCache(xmlConfiguration.getLong("cache[@maxEntriesInCache]", CacheConfiguration.DEFAULT_MAX_ENTRIES_IN_CACHE));
        cacheConfiguration.setMaxBytesLocalDisk(xmlConfiguration.getString("cache[@maxBytesLocalDisk]", "10g"));
        cacheConfiguration.setMaxBytesLocalHeap(xmlConfiguration.getString("cache[@maxBytesLocalHeap]", "1g"));
        cacheConfiguration.setMaxBytesLocalOffHeap(xmlConfiguration.getString("cache[@maxBytesLocalOffHeap]", "1000000"));
        cacheConfiguration.setOverflowToOffHeap(xmlConfiguration.getBoolean("cache[@overflowToOffHeap]", false));
        return cacheConfiguration;
    }

    private void initCacheManager(CacheConfiguration cacheConfiguration, String diskStorePath) {

        DiskStoreConfiguration dsc = new DiskStoreConfiguration();
        dsc.setPath(diskStorePath);
        Configuration cacheConfig = new Configuration();
        cacheConfig.addDiskStore(dsc);
        cacheConfiguration.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU);
        cacheConfiguration.persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.LOCALTEMPSWAP));

        cacheManager = new net.sf.ehcache.CacheManager(cacheConfig);
        lockWriteCache();
        imageCache = new Cache(cacheConfiguration);
        cacheManager.addCache(imageCache);
        cacheConfig.setDefaultCacheConfiguration(cacheConfiguration);
        unlockWriteCache();
    }

    private void shutdownCacheIfNeeded() {
        if (imageCache != null ) {
            cacheManager.removeAllCaches();
            cacheManager.shutdown();
        }
    }

    public boolean isCacheEnabled() {
        return getImageCache() != null;
    }

    public Cache getImageCache() {
        return imageCache;
    }

    public void lockWriteCache() {
        cacheLock.writeLock().lock();
    }

    public void unlockWriteCache() {
        cacheLock.writeLock().unlock();
    }

    public Element getImageFromCache(String pathInfo) {
        return imageCache.get(pathInfo);
    }

    public void shutdownCacheManager() {
        cacheManager.shutdown();
    }

    public void putToCacheIfCacheExists(String path, byte[] data, Map<String, String> responseHeaders) {
        if (isCacheEnabled()) {
            lockWriteCache();
            try {
                imageCache.put(new net.sf.ehcache.Element(path, new CachedImage(data, responseHeaders)));
            } finally {
                unlockWriteCache();
            }
        }
    }

    public void lockReadCache() {
        cacheLock.readLock().lock();
    }

    public void unlockReadCache() {
        cacheLock.readLock().unlock();
    }

    private static class CacheManagerHolder {
        public final static CacheManager instance = new CacheManager();

        private CacheManagerHolder(){
        }
    }

    public static CacheManager getInstance() {
        return CacheManagerHolder.instance;
    }

}
