package com.n11.imic;

import com.n11.imic.cache.CacheManager;
import com.n11.imic.cache.CachedImage;
import com.n11.imic.config.ImageScalerConfiguration;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import net.sf.ehcache.Element;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.HttpHeaders;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageScalerServlet extends ProxyServlet {

    private static final long serialVersionUID = -4875037205150829084L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageScalerServlet.class);

    private SimpleDateFormat dateFormat;

    private static final Pattern ptScaleOriginalImage = Pattern.compile("^/([^/]+)/(.+)$");

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        ImageScalerConfiguration.getInstance().initConfiguration();
        CacheManager.getInstance().initConfiguration();
        ImageIO.scanForPlugins();
        dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (cacheExists(request) && useCache(request, response)) {
            return;
        }
        serviceFromFileSystem(request, response);
    }

    private void serviceFromFileSystem(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Matcher mtScaleImage = ptScaleOriginalImage.matcher(request.getPathInfo());
        if (mtScaleImage.matches()) {
            String scaleParam = mtScaleImage.group(1);
            if (ImageScalerConfiguration.getInstance().getDefaultScaleParameters().containsKey(scaleParam)) {
                ScalerParam scaler = ImageScalerConfiguration.getInstance().getDefaultScaleParameters().get(scaleParam).copy();
                setAnimatedGifMode(request, scaler);
                serviceWithLocalStorage(request, response, scaler, mtScaleImage.group(2));
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private boolean cacheExists(HttpServletRequest servletRequest) {
        return CacheManager.getInstance().isCacheEnabled()
                && !("no-cache".equalsIgnoreCase(servletRequest.getHeader("Cache-Control")));
    }

    private boolean useCache(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

        CacheManager.getInstance().lockReadCache();
        Element cachedImage = CacheManager.getInstance().getImageFromCache(servletRequest.getPathInfo());
        CacheManager.getInstance().unlockReadCache();
        if (cachedImage != null) {
            Object obj = cachedImage.getObjectValue();
            CachedImage ci = (CachedImage) obj;
            for (Map.Entry<String, String> headerEntry : ci.getHttpHeaders().entrySet()) {
                servletResponse.setHeader(headerEntry.getKey(), headerEntry.getKey().equals(HttpHeaders.CONTENT_LENGTH) ? String.valueOf(ci.getImageDataLength()) : headerEntry.getValue());
            }
            IOUtils.copy(new ByteArrayInputStream(ci.getImageData()), servletResponse.getOutputStream());
            return true;
        }

        return false;
    }

    private void setAnimatedGifMode(HttpServletRequest servletRequest, ScalerParam requestScalingParameters) {
        String agifmode = servletRequest.getParameter("agifmode");
        requestScalingParameters.setAnimatedGifMode(AnimatedGIFMode.valueFrom(agifmode));
    }

    private void serviceWithLocalStorage(HttpServletRequest servletRequest, HttpServletResponse servletResponse, ScalerParam requestScalingParameters, String url) throws IOException {
        FileInputStream originalImageFile = null;
        Map<String, String> responseHeaders = new HashMap<String, String>();
        try {
            String requestedFile = FilenameUtils.normalize(ImageScalerConfiguration.getInstance().getSourceLocation() + File.separator + URLDecoder.decode(url, "UTF-8"));
            File file = new File(requestedFile);
            originalImageFile = new FileInputStream(file);
            String contentType = findContentType(file, originalImageFile);

            addHeadersToResponse(responseHeaders, requestScalingParameters, file, contentType);

            byte[] data = copyResponseEntity(originalImageFile, responseHeaders, requestScalingParameters, contentType, file.length());
            if (ArrayUtils.isNotEmpty(data)) {
                CacheManager.getInstance().putToCacheIfCacheExists(servletRequest.getPathInfo(), data, responseHeaders);
                IOUtils.copy(new ByteArrayInputStream(data), servletResponse.getOutputStream());
            } else {
                originalImageFile.getChannel().position(0);
                IOUtils.copy(originalImageFile, servletResponse.getOutputStream());
            }
        } catch (Exception e) {
            LOGGER.error("Error servicing the file ",e);
        } finally {
            for (Map.Entry<String, String> e : responseHeaders.entrySet()) {
                servletResponse.setHeader(e.getKey(), e.getValue());
            }
            closeQuietly(originalImageFile);
            closeQuietly(servletResponse.getOutputStream());
        }
    }

    private void addHeadersToResponse(Map<String, String> responseHeaders, ScalerParam requestScalingParameters, File file, String contentType) {
        if (contentType.matches("^image/gif")) {
            responseHeaders.put(HttpHeaders.CONTENT_TYPE, "image/png");
        }
        responseHeaders.put(HttpHeaders.CONTENT_TYPE, contentType);
        responseHeaders.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()));
        responseHeaders.put(HttpHeaders.CACHE_CONTROL, "max-age=" + requestScalingParameters.getHttpExpires());
        responseHeaders.put(HttpHeaders.LAST_MODIFIED, dateFormat.format(file.lastModified()));
        responseHeaders.put(HttpHeaders.EXPIRES, dateFormat.format(DateUtils.addSeconds(new Date(), requestScalingParameters.getHttpExpires())));
        responseHeaders.put(HttpHeaders.AGE, "0");
    }

    private String findContentType(File file, FileInputStream originalImageFile) throws IOException {
        ContentInfoUtil contentInfoUtil = new ContentInfoUtil();
        ContentInfo contentInfo = contentInfoUtil.findMatch(originalImageFile);
        String contentType = contentInfo.getContentType().getMimeType();
        originalImageFile.getChannel().position(0);

        if (contentType == null) {
            contentType = getServletContext().getMimeType(file.getName());
        }
        if (contentType == null) {
            contentType = URLConnection.guessContentTypeFromStream(originalImageFile);
            originalImageFile.getChannel().position(0);
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return contentType;
    }

    private byte[] copyResponseEntity(InputStream originalData, Map<String, String> headers, ScalerParam requestScalingParameters, String contentType, long contentLength) {
        try {
            BufferedImage result = ImageScaler.scaleImage(originalData, contentType, requestScalingParameters);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.abs(requestScalingParameters.getTargetWidth() * requestScalingParameters.getTargetHeight() * 2));
            ImageScaler.writeImageToStream(result, baos, contentType, requestScalingParameters);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(baos.size()));
            result.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            LOGGER.error("Exception Occured while copying the response entity", e);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
            headers.put(HttpHeaders.CONTENT_TYPE, contentType);
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
    }


    @Override
    public void destroy() {
        CacheManager.getInstance().shutdownCacheManager();
        super.destroy();
    }

    @Override
    protected void closeQuietly(Closeable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (IOException e) {
            LOGGER.error("An error occurred while closing resource.", e);
        }
    }
}
