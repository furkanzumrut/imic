package com.n11.imic.config;

import com.n11.imic.ScalerParam;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.imgscalr.Scalr;

import java.util.HashMap;
import java.util.Map;

public class ImageScalerConfiguration {

    private final Map<String, ScalerParam> defaultScaleParameters = new HashMap<String, ScalerParam>();

    private String sourceLocation;

    private ImageScalerConfiguration() {
    }

    public void initConfiguration() {
        try {
            XMLConfiguration configuration = new XMLConfiguration(ImageScalerConfigFileLocator.findConfigFile());
            readConfiguration(configuration);
        } catch (ConfigurationException ce) {
            throw new IllegalArgumentException("Cannot start the app, are you sure you have the necessary configuration?", ce);
        }
    }

    public void readConfiguration(XMLConfiguration xmlConfiguration) {
        sourceLocation = xmlConfiguration.getString("source[@location]");
        initPaths(xmlConfiguration);
    }

    private void initPaths(XMLConfiguration xmlConfiguration) {
        defaultScaleParameters.clear();
        HierarchicalConfiguration scales = xmlConfiguration.configurationAt("scales");
        for (Object configuration : scales.configurationsAt("scale")) {
            SubnodeConfiguration scaleConfig = (SubnodeConfiguration) configuration;
            defaultScaleParameters.put(scaleConfig.getString("[@path]"), new ScalerParam()
                            .withWidth(scaleConfig.getInt("[@width]", ScalerParam.UNDEFINED_SIZE))
                            .withHasPadding(scaleConfig.getBoolean("[@hasPadding]", false))
                            .withHeight(scaleConfig.getInt("[@height]", ScalerParam.UNDEFINED_SIZE))
                            .withUpScale(scaleConfig.getBoolean("[@upscale]", false))
                            .withPadding(scaleConfig.getInt("[@padding]", ScalerParam.UNDEFINED_SIZE))
                            .withPaddingColor(scaleConfig.getString("[@background]", "#FFFFFF"))
                            .withQuality(scaleConfig.getFloat("[@quality]", 80f))
                            .withMethod(Scalr.Method.valueOf(scaleConfig.getString("[@method]", Scalr.Method.QUALITY.name())))
                            .withProgressiveMode(scaleConfig.getBoolean("[@progressivemode]", false))
            );
        }
    }

    private static class ImageScalerConfigurationHolder {
        public final static ImageScalerConfiguration instance = new ImageScalerConfiguration();
        private ImageScalerConfigurationHolder(){
        }
    }

    public static ImageScalerConfiguration getInstance() {
        return ImageScalerConfigurationHolder.instance;
    }

    public Map<String, ScalerParam> getDefaultScaleParameters() {
        return defaultScaleParameters;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }
}
