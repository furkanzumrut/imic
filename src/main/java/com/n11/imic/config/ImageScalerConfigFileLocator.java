package com.n11.imic.config;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

public class ImageScalerConfigFileLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageScalerConfigFileLocator.class);

    private static final String USER_DEFINED_CONFIGFILE_PARAM = "config.file";

    private ImageScalerConfigFileLocator() {
    }

    private static final String[] predefinedConfigFilePaths = new String[]{
            "/etc/imic/imic.xml",
    };

    public static File findConfigFile() throws ConfigurationException {
        for (String configFilePath : predefinedConfigFilePaths) {
            File file = new File(configFilePath);
            if (properFileToRead(file)) {
                return file;
            }
        }

        LOGGER.info("Could not found any configuration file in predefined paths:" + Arrays.deepToString(predefinedConfigFilePaths));
        LOGGER.info("Looking at VM's '" + USER_DEFINED_CONFIGFILE_PARAM + "' parameter for configuration file...");

        String userDefinedConfigFilePath = System.getProperty("config.file");
        if (userDefinedConfigFilePath == null) {
            throw new ConfigurationException("Could not found '"+ USER_DEFINED_CONFIGFILE_PARAM +"' parameter for configuration file.");
        }
        File userDefinedConfigFile = new File(userDefinedConfigFilePath);
        if (properFileToRead(userDefinedConfigFile)) {
            return userDefinedConfigFile;
        } else {
            throw new ConfigurationException("User defined config file:'"+ userDefinedConfigFile.getAbsolutePath() +"' is not proper to read.");
        }
    }

    private static boolean properFileToRead(File file) {
        return file.isFile() && file.canRead();
    }

}