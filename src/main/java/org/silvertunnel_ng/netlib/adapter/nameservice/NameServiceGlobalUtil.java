//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.silvertunnel_ng.netlib.adapter.nameservice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.silvertunnel_ng.netlib.api.NetAddressNameService;
import org.silvertunnel_ng.netlib.nameservice.inetaddressimpl.DefaultIpNetAddressNameService;
import org.silvertunnel_ng.netlib.nameservice.mock.NopNetAddressNameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.InetAddressCachePolicy;
import sun.net.spi.nameservice.NameService;

public class NameServiceGlobalUtil {
  private static final Logger LOG = LoggerFactory.getLogger(NameServiceGlobalUtil.class);
  private static boolean initialized = false;
  private static boolean initializedWithSuccess = false;
  private static final long CACHE_TIMEOUT_MILLIS = 11000L;
  private static List<NameService> oldNameServices;

  public NameServiceGlobalUtil() {}

  public static synchronized void initNameService() throws IllegalStateException {
    if (initialized) {
      if (!isNopNetAddressNameServiceInstalled()) {
        setIpNetAddressNameService(NopNetAddressNameService.getInstance());
      }

      LOG.debug("initialized");
    } else {
      System.setProperty("sun.net.spi.nameservice.provider.1", "dns,NetlibNameService");
      System.setProperty("sun.net.inetaddr.ttl", "0");
      System.setProperty("sun.net.inetaddr.negative.ttl", "0");
      System.setProperty(
          "org.silvertunnel_ng.netlib.nameservice",
          "org.silvertunnel_ng.netlib.nameservice.mock.NopNetAddressNameService");
      initialized = true;
    }

    initializedWithSuccess = isNopNetAddressNameServiceInstalled();
    if (initializedWithSuccess) {
      LOG.info("Installation of NameService adapter with NopNetAddressNameService was successful");
    } else {
      initNameServiceHardway();
      initializedWithSuccess = isNopNetAddressNameServiceInstalled();
      if (!initializedWithSuccess) {
        String msg =
            "Installation of NameService adapter with NopNetAddressNameService failed: probably the method NameServiceGlobalUtil.initNameService() is called too late, i.e. after first usage of java.net.InetAddress";
        LOG.error(
            "Installation of NameService adapter with NopNetAddressNameService failed: probably the method NameServiceGlobalUtil.initNameService() is called too late, i.e. after first usage of java.net.InetAddress");
        throw new IllegalStateException(
            "Installation of NameService adapter with NopNetAddressNameService failed: probably the method NameServiceGlobalUtil.initNameService() is called too late, i.e. after first usage of java.net.InetAddress");
      }

      LOG.info(
          "Installation of NameService adapter with NopNetAddressNameService was successful (hard way)");
    }
  }

  private static void initNameServiceHardway() {
    try {
      Field field = InetAddress.class.getDeclaredField("nameServices");
      field.setAccessible(true);
      oldNameServices = (List) field.get((Object) null);
      String provider = null;
      String propPrefix = "sun.net.spi.nameservice.provider.";
      int n = 1;
      List<NameService> nameServices = new ArrayList();

      Method m;
      NameService ns;
      for (provider = System.getProperty(propPrefix + n);
          provider != null;
          provider = System.getProperty(propPrefix + n)) {
        m = InetAddress.class.getDeclaredMethod("createNSProvider", String.class);
        m.setAccessible(true);
        ns = (NameService) m.invoke((Object) null, provider);
        if (ns != null) {
          nameServices.add(ns);
        }

        ++n;
      }

      if (nameServices.size() == 0) {
        m = InetAddress.class.getDeclaredMethod("createNSProvider", String.class);
        m.setAccessible(true);
        ns = (NameService) m.invoke((Object) null, "default");
        nameServices.add(ns);
      }

      field.set((Object) null, nameServices);
      Field fieldCache = InetAddressCachePolicy.class.getDeclaredField("cachePolicy");
      fieldCache.setAccessible(true);
      fieldCache.set((Object) null, 0);
      Field fieldCacheNeg = InetAddressCachePolicy.class.getDeclaredField("negativeCachePolicy");
      fieldCacheNeg.setAccessible(true);
      fieldCacheNeg.set((Object) null, 0);
    } catch (Exception var7) {
      LOG.debug("Hardway init doesnt work. got Exception : {}", var7, var7);
    }
  }

  public static void resetInetAddress() {
    if (oldNameServices != null) {
      try {
        Field field = InetAddress.class.getDeclaredField("nameServices");
        field.setAccessible(true);
        field.set((Object) null, oldNameServices);
      } catch (Exception var1) {
        LOG.warn("Could not reset InetAddress due to exception", var1);
      }
    }
  }

  public static boolean isNopNetAddressNameServiceInstalled() {
    InetAddress[] address;
    try {
      address = InetAddress.getAllByName("dnstest.silvertunnel-ng.org"); // invalid host
      return false;
    } catch (UnknownHostException var2) {
      try {
        address = InetAddress.getAllByName(NopNetAddressNameService.CHECKER_NAME); // EDIT SAMOURAI
        if (address == null) {
          LOG.error("InetAddress.getAllByName() returned null as address (but this is wrong)");
          return false;
        } else if (address.length != 1) {
          LOG.error("InetAddress.getAllByName() returned array of wrong size={}", address.length);
          return false;
        } else if (Arrays.equals(
            address[0].getAddress(), NopNetAddressNameService.CHECKER_IP[0].getIpaddress())) {
          return true;
        } else {
          LOG.error(
              "InetAddress.getAllByName() returned wrong IP address={}",
              Arrays.toString(address[0].getAddress()));
          return false;
        }
      } catch (Exception var1) {
        LOG.error("InetAddress.getAllByName() throwed unexpected excpetion={}", var1, var1);
        return false;
      }
    }
  }

  public static synchronized void setIpNetAddressNameService(
      NetAddressNameService lowerNetAddressNameService) throws IllegalStateException {
    if (!initialized) {
      throw new IllegalStateException("initNameService() must be called first (but was not)");
    } else {
      NetlibNameServiceDescriptor.getSwitchingNetAddressNameService()
          .setLowerNetAddressNameService(lowerNetAddressNameService);
    }
  }

  public static long getCacheTimeoutMillis() {
    return 11000L;
  }

  public static boolean isDefaultIpNetAddressNameServiceActive() {
    return NetlibNameServiceDescriptor.getSwitchingNetAddressNameService()
        .getLowerNetAddressNameServiceClass()
        .equals(DefaultIpNetAddressNameService.class.getCanonicalName());
  }

  public static void activateDefaultIpNetAddressNameService() {
    if (!initialized) {
      initNameService();
    }

    setIpNetAddressNameService(DefaultIpNetAddressNameService.getInstance());
  }
}
