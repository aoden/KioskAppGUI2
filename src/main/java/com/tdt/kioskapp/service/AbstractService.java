package com.tdt.kioskapp.service;


import com.tdt.kioskapp.Application;

/**
 * AbstractService
 *
 * @author aoden
 */
public abstract class AbstractService {

    public static volatile String TEMP_DIR = "temp";
    public static final String MANIFEST_JSON = "manifest.json";
    static final String SECRET = "demo_client";
    static final String ID = "demo_client";
    public static final String path = Application.class.getProtectionDomain().getCodeSource().getLocation().getPath();
}
