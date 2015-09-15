package com.n11.imic;

import com.n11.imic.cache.CacheManager;
import com.n11.imic.config.ImageScalerConfiguration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ReloadConfigServlet extends HttpServlet {

    private static final long serialVersionUID = -4072221346657681089L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ImageScalerConfiguration.getInstance().initConfiguration();
        CacheManager.getInstance().initConfiguration();
    }
}
