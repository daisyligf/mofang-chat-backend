package com.mofang.chat.backend.job.worker.impl;

import org.json.JSONObject;

import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.backend.job.worker.JobWorker;
import com.mofang.chat.business.model.SysMessageNotify;
import com.mofang.chat.business.mysql.SysMessageNotifyDao;
import com.mofang.chat.business.mysql.impl.SysMessageNotifyDaoImpl;
import com.mofang.chat.business.redis.PushQueueRedis;
import com.mofang.chat.business.redis.SysMessageNotifyRedis;
import com.mofang.chat.business.redis.impl.PushQueueRedisImpl;
import com.mofang.chat.business.redis.impl.SysMessageNotifyRedisImpl;
import com.mofang.chat.business.sysconf.common.PushDataType;
import com.mofang.chat.business.sysconf.common.SysMessageNotifyStatus;

/**
 * 
 * @author zhaodx
 *
 */
public class SysMessageNotifyWorker implements JobWorker
{
	private SysMessageNotifyRedis sysMessageNotifyRedis = SysMessageNotifyRedisImpl.getInstance();
	private SysMessageNotifyDao sysMessageNotifyDao = SysMessageNotifyDaoImpl.getInstance();
	private PushQueueRedis pushRedis = PushQueueRedisImpl.getInstance();
	private JSONObject json;
	
	public SysMessageNotifyWorker(JSONObject json)
	{
		this.json = json;
	}

	@Override
	public void execute()
	{
		SysMessageNotify model = SysMessageNotify.buildByJson(json);
		if(null == model)
			return;
		
		try
		{
			///保存到redis
			model.setStatus(SysMessageNotifyStatus.UNREAD);
			sysMessageNotifyRedis.save(model);
			
			///构建push消息
			JSONObject pushMsg = new JSONObject();
			pushMsg.put("push_data_type", PushDataType.SYS_MESSAGE_NOTIFY);
			pushMsg.put("to_uid", model.getUserId());
			pushMsg.put("notify_id", model.getNotifyId());
			///添加到push queue
			pushRedis.put(pushMsg.toString());
			GlobalObject.INFO_LOG.info("SysMessageNotifyWorker add push queue message:" + pushMsg.toString());
			
			///保存到mysql
			sysMessageNotifyDao.add(model);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at SysMessageNotifyWorker.execute throw an error.", e);
		}
	}
}