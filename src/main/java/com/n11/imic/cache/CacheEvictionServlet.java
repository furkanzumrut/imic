package com.n11.imic.cache;

import net.sf.ehcache.statistics.StatisticsGateway;
import org.apache.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CacheEvictionServlet extends HttpServlet {

    private static final long serialVersionUID = -3163626556212002775L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StatisticsGateway statistics = CacheManager.getInstance().getImageCache().getStatistics();
        resp.getWriter().printf("Cache has %d objects: %d objects (%d bytes) in memory store, %d objects (%d bytes) in disk store.%n",
                statistics.getSize(),
                statistics.getLocalHeapSize(),
                statistics.getLocalHeapSizeInBytes(),
                statistics.getLocalDiskSize(),
                statistics.getLocalDiskSizeInBytes()
        );
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (removeAllKeysRequest(req)) {
            removeAll(resp);
        } else if (removeKeysRequest(req)) {
            removeKeys(req, resp);
        } else {
            removeKey(req, resp);
        }
    }

    private void removeKey(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String key = req.getParameter("key").replaceFirst("http://([a-z0-9A-Z].*)$", "http:/$1");
        if (CacheManager.getInstance().getImageCache().remove(key)) {
            resp.getWriter().printf("Successfully removed requested key '%s' from cache.", key);
            resp.setStatus(HttpStatus.SC_OK);
        } else {
            resp.getWriter().printf("Remove failed, requested key '%s' not found in cache.", key);
            resp.setStatus(HttpStatus.SC_NOT_FOUND);
        }
    }

    private void removeKeys(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyMatcher = req.getParameter("key").replace("*", "");
        List<String> toRemove = new ArrayList<String>();
        for (Object key : CacheManager.getInstance().getImageCache().getKeysNoDuplicateCheck()) {
            if (((String) key).startsWith(keyMatcher)) {
                toRemove.add((String) key);
            }
        }
        CacheManager.getInstance().getImageCache().removeAll(toRemove);
        for (String removed : toRemove) {
            resp.getWriter().println(String.format("Successfully removed requested key '%s' from cache.", removed));
        }
        resp.setStatus(HttpStatus.SC_OK);
    }

    private boolean removeKeysRequest(HttpServletRequest req) {
        return req.getParameter("key").endsWith("*");
    }

    private void removeAll(HttpServletResponse resp) throws IOException {
        CacheManager.getInstance().getImageCache().removeAll();
        resp.getWriter().println("Cache cleared.");
        resp.setStatus(HttpStatus.SC_OK);
    }

    private boolean removeAllKeysRequest(HttpServletRequest req) {
        return "*".equals(req.getParameter("key"));
    }
}
