
package org.smslib.core;

public class Settings
{
	public static final String LIBRARY_INFO = "SMSLib - A universal API for sms messaging";

	public static final String LIBRARY_LICENSE = "This software is distributed under the terms of the\nApache v2.0 License (http://www.apache.org/licenses/LICENSE-2.0.html).";

	public static final String LIBRARY_COPYRIGHT = "Copyright (c) 2002-2014, smslib.org";

	public static final String LIBRARY_VERSION = "dev-SNAPSHOT";

	public static int httpServerPort = 8001;

	public static String httpServerACLStatus = "127.0.0.1/32";

	public static int serviceDispatcherQueueTimeout = 1000;

	public static int serviceDispatcherYield = 0;

	public static int gatewayDispatcherQueueTimeout = 1000;

	public static int gatewayDispatcherYield = 0;

	public static int callbackDispatcherQueueTimeout = 1000;

	public static int callbackDispatcherYield = 0;

	public static int daemonDispatcherYield = 10000;

	public static boolean keepOutboundMessagesInQueue = true;

	public static int queueCallbackLowThreshold = 10;

	public static int queueCallbackHighThreshold = 50;

	public static int hoursToRetainOrphanedMessageParts = 72;

	public static boolean deleteMessagesAfterCallback = false;

	public static int modemPollingInterval = 15000;

	public static void loadSettings()
	{
		if (System.getProperty("smslib.httpserver.port") != null) httpServerPort = Integer.parseInt(System.getProperty("smslib.httpserver.port"));
		if (System.getProperty("smslib.httpserver.acl.status") != null) httpServerACLStatus = System.getProperty("smslib.httpserver.acl.status");
	}
}
