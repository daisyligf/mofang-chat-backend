package com.mofang.chat.backend;

import com.mofang.chat.backend.cron.CrontabBootstrap;
import com.mofang.chat.backend.init.Initializer;
import com.mofang.chat.backend.init.impl.MainInitializer;
import com.mofang.chat.backend.job.JobServer;

/**
 * 
 * @author zhaodx
 *
 */
public class Server
{
	public static void main(String[] args) 
	{
		//String configpath = "/Users/milo/document/workspace/mofang.chat.backend/src/main/resources/config.ini";
		
		if(args.length <= 0)
		{
			System.out.println("usage:java -server -Xms1024m -Xmx1024m -jar mofang-chat-backend.jar configpath");
			System.exit(1);
		}
		String configpath = args[0];
		
		try
		{
			///服务器初始化
			System.out.println("prepare to initializing config......");
			Initializer initializer = new MainInitializer(configpath);
			initializer.init();
			System.out.println("initialize config completed!");
			
			///启动定时任务
			Thread timeJob = new Thread(new CrontabBootstrap());
			timeJob.start();
			
			///启动服务
			JobServer server = new JobServer();
			System.out.println("Backend Server Start.");
			server.start();
		}
		catch(Exception e)
		{
			System.out.println("backend server start error. message:" + e.getMessage());
			e.printStackTrace();
		}
	}
}