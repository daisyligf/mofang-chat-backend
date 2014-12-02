package com.mofang.chat.backend.cron;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.mofang.chat.backend.cron.task.AcAutoTask;
import com.mofang.chat.backend.cron.task.RoomUserTask;
import com.mofang.chat.backend.global.GlobalConfig;

/**
 * 定时任务启动类
 * @author zhaodx
 *
 */
public class CrontabBootstrap implements Runnable
{
	@Override
	public void run()
	{
		TaskEntity acTask = getAcAutoTask();
		TaskEntity roomUserTask = getRoomUserTask();
		CrontabManager cron = new CrontabManager();
		cron.add(acTask);
		cron.add(roomUserTask);
		cron.execute();
	}
	
	/**
	 * 构建AC自动机的定时任务
	 * @return
	 */
	private TaskEntity getAcAutoTask()
	{
		TaskEntity entity = new TaskEntity();
		long oneDay = 24 * 60 * 60 * 1000;  
	    long initDelay  = getTimeMillis(GlobalConfig.AC_AUTO_TASK_TIME) - System.currentTimeMillis();  
	    initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;  
		entity.setInitialDelay(initDelay);
		entity.setPeriod(oneDay);
		entity.setUnit(TimeUnit.MILLISECONDS);
		entity.setTask(new AcAutoTask());
		return entity;
	}
	
	/**
	 * 构建订阅房间用户列表的定时任务
	 * @return
	 */
	private TaskEntity getRoomUserTask()
	{
		TaskEntity entity = new TaskEntity();
		long oneDay = 24 * 60 * 60 * 1000;  
	    long initDelay  = getTimeMillis(GlobalConfig.ROOM_USER_TASK_TIME) - System.currentTimeMillis();  
	    initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;  
		entity.setInitialDelay(initDelay);
		entity.setPeriod(oneDay);
		entity.setUnit(TimeUnit.MILLISECONDS);
		entity.setTask(new RoomUserTask());
		return entity;
	}
	
	/**
	 * 获取指定时间对应的毫秒数
	 * @param time "HH:mm:ss"
	 * @return
	 */
	private long getTimeMillis(String time) 
	{
		try
		{
			DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
			DateFormat dayFormat = new SimpleDateFormat("yy-MM-dd");
			Date curDate = dateFormat.parse(dayFormat.format(new Date()) + " " + time);
			return curDate.getTime();
		} 
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
		return 0;
	}
}