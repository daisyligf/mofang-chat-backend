package com.mofang.chat.backend.job;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mofang.chat.backend.global.GlobalObject;

/**
 * 
 * @author zhaodx
 *
 */
public class JobServer
{
	public void start()
	{
		try
		{
			int threads = Runtime.getRuntime().availableProcessors() * 2 + 1;
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			//ExecutorService executor = Executors.newCachedThreadPool();
			JobAcceptor acceptor = new JobAcceptor(executor);
			acceptor.start();
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at JobServer.start throw an error.", e);
		}
	}
}