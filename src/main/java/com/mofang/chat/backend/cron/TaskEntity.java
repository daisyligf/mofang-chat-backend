package com.mofang.chat.backend.cron;

import java.util.concurrent.TimeUnit;

/**
 * 
 * @author zhaodx
 *
 */
public class TaskEntity
{
	private Runnable task;
	private long initialDelay;
	private long period;
	private TimeUnit unit;
	
	public Runnable getTask()
	{
		return task;
	}
	
	public void setTask(Runnable task)
	{
		this.task = task;
	}
	
	public long getInitialDelay()
	{
		return initialDelay;
	}
	
	public void setInitialDelay(long initialDelay)
	{
		this.initialDelay = initialDelay;
	}
	
	public long getPeriod() 
	{
		return period;
	}
	
	public void setPeriod(long period)
	{
		this.period = period;
	}
	
	public TimeUnit getUnit() 
	{
		return unit;
	}
	
	public void setUnit(TimeUnit unit)
	{
		this.unit = unit;
	}
}