package com.mofang.chat.backend.job;

import org.json.JSONObject;

import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.backend.job.factory.JobWorkerFactory;
import com.mofang.chat.backend.job.worker.JobWorker;

public class JobHandler implements Runnable
{
	private String message;
	
	public JobHandler(String message)
	{
		this.message = message;
	}

	@Override
	public void run()
	{
		try
		{
			JSONObject json = new JSONObject(message);
			int dataType = json.optInt("write_data_type", 0);
			if(0 == dataType)
				return;
			
			JobWorker worker = JobWorkerFactory.getInstance(dataType, json);
			if(null == worker)
				return;
			
			worker.execute();
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at JobHandler.run throw an error.", e);
		}
	}
}