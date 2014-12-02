package com.mofang.chat.backend.job;

import java.util.concurrent.ExecutorService;

import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.business.redis.WriteQueueRedis;
import com.mofang.chat.business.redis.impl.WriteQueueRedisImpl;
import com.mofang.framework.util.StringUtil;

/**
 * 
 * @author zhaodx
 *
 */
public class JobAcceptor
{
	private WriteQueueRedis writeQueue = WriteQueueRedisImpl.getInstance();
	private ExecutorService executor = null;
	
	public JobAcceptor(ExecutorService executor)
	{
		this.executor = executor;
	}
	
	public void start()
	{
		try
		{
			while(true)
			{
				String message = writeQueue.get();
				if(StringUtil.isNullOrEmpty(message))
				{
					Thread.sleep(1);
					continue;
				}
				
				GlobalObject.INFO_LOG.info(message);
				JobHandler worker = new JobHandler(message);
				executor.execute(worker);
			}
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at JobAcceptor.start throw an error.", e);
		}
	}
}