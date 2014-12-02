package com.mofang.chat.backend.cron.task;

import com.mofang.chat.backend.util.acauto.AcAutoHelper;

/**
 * 
 * @author zhaodx
 *
 */
public class AcAutoTask implements Runnable
{
	@Override
	public void run()
	{
		AcAutoHelper.reload();
	}
}