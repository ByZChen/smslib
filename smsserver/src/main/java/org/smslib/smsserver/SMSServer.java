// SMSLib for Java v4
// A universal API for sms messaging.
//
// Copyright (C) 2002-2014, smslib.org
// For more information, visit http://smslib.org
// SMSLib is distributed under the terms of the Apache License version 2.0
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.smslib.smsserver;

import java.lang.reflect.Constructor;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smslib.Service;
import org.smslib.core.Settings;
import org.smslib.gateway.AbstractGateway;
import org.smslib.helper.Common;
import org.smslib.message.MsIsdn;
import org.smslib.routing.NumberRouter;
import org.smslib.smsserver.callback.DeliveryReportCallback;
import org.smslib.smsserver.callback.DequeueMessageCallback;
import org.smslib.smsserver.callback.GatewayStatusCallback;
import org.smslib.smsserver.callback.InboundCallCallback;
import org.smslib.smsserver.callback.InboundMessageCallback;
import org.smslib.smsserver.callback.MessageSentCallback;
import org.smslib.smsserver.callback.ServiceStatusCallback;
import org.smslib.smsserver.db.IDatabaseHandler;
import org.smslib.smsserver.db.MySQLDatabaseHandler;
import org.smslib.smsserver.db.data.GatewayDefinition;
import org.smslib.smsserver.db.data.NumberRouteDefinition;
import org.smslib.smsserver.hook.PreQueueHook;

public class SMSServer
{
	static Logger logger = LoggerFactory.getLogger(SMSServer.class);

	private static final SMSServer smsserver = new SMSServer();

	String profile = "";

	public Object LOCK = new Object();

	OutboundServiceThread outboundService;

	IDatabaseHandler databaseHandler;

	public static SMSServer getInstance()
	{
		return smsserver;
	}

	public SMSServer()
	{
	}

	public OutboundServiceThread getOutboundServiceThread()
	{
		return this.outboundService;
	}

	public void startup() throws Exception
	{
		Runtime.getRuntime().addShutdownHook(new ShutdownThread());
		Service.getInstance().setServiceStatusCallback(new ServiceStatusCallback());
		Service.getInstance().setGatewayStatusCallback(new GatewayStatusCallback());
		Service.getInstance().setMessageSentCallback(new MessageSentCallback());
		Service.getInstance().setDequeueMessageCallback(new DequeueMessageCallback());
		Service.getInstance().setPreQueueHook(new PreQueueHook());
		Service.getInstance().setInboundMessageCallback(new InboundMessageCallback());
		Service.getInstance().setDeliveryReportCallback(new DeliveryReportCallback());
		Service.getInstance().setInboundCallCallback(new InboundCallCallback());
		Service.getInstance().start();
		loadGatewayDefinitions();
		//loadGroups();
		loadNumberRoutes();
		this.outboundService = new OutboundServiceThread();
	}

	public void shutdown()
	{
		new ShutdownThread().start();
	}

	private void loadGatewayDefinitions() throws Exception
	{
		Collection<GatewayDefinition> gateways = databaseHandler.getGatewayDefinitions(profile);
		for (GatewayDefinition gd : gateways)
		{
			logger.info("Registering gateway: " + gd.gatewayId);
			try
			{
				String[] parms = new String[6];
				parms[0] = gd.p0;
				parms[1] = gd.p1;
				parms[2] = gd.p2;
				parms[3] = gd.p3;
				parms[4] = gd.p4;
				parms[5] = gd.p5;
				Object[] args = new Object[] { gd.gatewayId, parms };
				Class<?>[] argsClass = new Class[] { String.class, String[].class };
				Class<?> c = Class.forName(gd.className);
				Constructor<?> constructor = c.getConstructor(argsClass);
				AbstractGateway g = (AbstractGateway) constructor.newInstance(args);
				if (!Common.isNullOrEmpty(gd.senderId)) g.setSenderAddress(new MsIsdn(gd.senderId));
				g.setPriority(gd.priority);
				g.setMaxMessageParts(gd.maxMessageParts);
				g.setRequestDeliveryReport(gd.requestDeliveryReport);
				Service.getInstance().registerGateway(g);
			}
			catch (Exception e)
			{
				logger.error("Gateway " + gd.gatewayId + " did not start properly!", e);
			}
		}
	}

/*
	private void loadGroups() throws ClassNotFoundException, SQLException, InterruptedException
	{
		Connection db = getDbConnection();
		Statement s1 = db.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		ResultSet rs1 = s1.executeQuery("select id, group_name, group_description from smslib_groups where (profile = '*' or profile = '" + SMSServer.getInstance().profile + "')");
		while (rs1.next())
		{
			int groupId = rs1.getInt(1);
			String groupName = rs1.getString(2).trim();
			String groupDescription = rs1.getString(3).trim();
			Group group = new Group(groupName, groupDescription);
			PreparedStatement s2 = db.prepareStatement("select address from smslib_group_recipients where group_id = ?");
			s2.setInt(1, groupId);
			ResultSet rs2 = s2.executeQuery();
			while (rs2.next())
				group.addAddress(new MsIsdn(rs2.getString(1).trim()));
			rs2.close();
			Service.getInstance().getGroupManager().addGroup(group);
		}
		rs1.close();
		s1.close();
		db.close();
	}
*/

	private void loadNumberRoutes() throws Exception
	{
		NumberRouter nr = new NumberRouter();
		Collection<NumberRouteDefinition> routes = databaseHandler.getNumberRouteDefinitions(profile);
		for (NumberRouteDefinition r : routes)
		{
			AbstractGateway g = Service.getInstance().getGatewayById(r.gatewayId);
			if (g == null) logger.error("Unknown gateway in number routes: " + r.gatewayId);
			else nr.addRule(r.addressRegex, g);
		}
		if (nr.getRules().size() > 0) Service.getInstance().setRouter(nr);
	}

	public static void main(String[] args)
	{
		logger.info("SMSServer Application - a database driver application based on SMSLib.");
		logger.info("SMSLib Version: " + Settings.LIBRARY_VERSION);
		logger.info(Settings.LIBRARY_INFO);
		logger.info(Settings.LIBRARY_COPYRIGHT);
		logger.info(Settings.LIBRARY_LICENSE);
		logger.info("For more information, visit http://smslib.org");
		logger.info("OS Version: " + System.getProperty("os.name") + " / " + System.getProperty("os.arch") + " / " + System.getProperty("os.version"));
		logger.info("JAVA Version: " + System.getProperty("java.version"));
		logger.info("JAVA Runtime Version: " + System.getProperty("java.runtime.version"));
		logger.info("JAVA Vendor: " + System.getProperty("java.vm.vendor"));
		logger.info("JAVA Class Path: " + System.getProperty("java.class.path"));
		logger.info("");
		try
		{
			try
			{
				String dbUrl = "";

				String dbDriver = "";

				String dbUsername = "";

				String dbPassword = "";

				int i = 0;
				while (i < args.length)
				{
					if (args[i].equalsIgnoreCase("-url")) dbUrl = args[++i];
					else if (args[i].equalsIgnoreCase("-driver")) dbDriver = args[++i];
					else if (args[i].equalsIgnoreCase("-username")) dbUsername = args[++i];
					else if (args[i].equalsIgnoreCase("-password")) dbPassword = args[++i];
					else if (args[i].equalsIgnoreCase("-profile")) SMSServer.getInstance().profile = args[++i];
					i++;
				}
				if (dbUrl.length() == 0 || dbDriver.length() == 0 || dbUsername.length() == 0) throw new IllegalArgumentException();
				if (dbDriver.equalsIgnoreCase("com.mysql.jdbc.Driver")) getInstance().databaseHandler = new MySQLDatabaseHandler(dbUrl, dbDriver, dbUsername, dbPassword);
				SMSServer.getInstance().startup();
			}
			catch (IllegalArgumentException e)
			{
				logger.info("Illegal / incorrect arguments!");
				logger.info("Parameters: -url 'db-url' -driver 'db-driver' -username 'db-username' -password 'db-password' [-profile 'profile']");
			}
		}
		catch (Exception e)
		{
			logger.error("Unhandled exception!", e);
			System.exit(1);
		}
	}
}
