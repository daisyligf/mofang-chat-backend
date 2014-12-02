package com.mofang.chat.backend.job.worker.impl;

import org.json.JSONObject;

import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.backend.job.worker.JobWorker;
import com.mofang.chat.business.model.PostReplyNotify;
import com.mofang.chat.business.mysql.PostReplyNotifyDao;
import com.mofang.chat.business.mysql.impl.PostReplyNotifyDaoImpl;
import com.mofang.chat.business.redis.PostReplyNotifyRedis;
import com.mofang.chat.business.redis.PushQueueRedis;
import com.mofang.chat.business.redis.impl.PostReplyNotifyRedisImpl;
import com.mofang.chat.business.redis.impl.PushQueueRedisImpl;
import com.mofang.chat.business.sysconf.common.PushDataType;

public class PostReplyNotifyWorker implements JobWorker
{
	private PostReplyNotifyRedis postReplyNotifyRedis = PostReplyNotifyRedisImpl.getInstance();
	private PostReplyNotifyDao postReplyNotifyDao = PostReplyNotifyDaoImpl.getInstance();
	private PushQueueRedis pushRedis = PushQueueRedisImpl.getInstance();
	private JSONObject json;
	
	public PostReplyNotifyWorker(JSONObject json)
	{
		this.json = json;
	}

	@Override
	public void execute()
	{
		PostReplyNotify model = PostReplyNotify.buildByJson(json);
		if(null == model)
			return;
		
		try
		{
			///保存到redis
			postReplyNotifyRedis.save(model);
			
			///构建push消息
			JSONObject pushMsg = new JSONObject();
			pushMsg.put("push_data_type", PushDataType.POST_REPLY_NOTIFY);
			pushMsg.put("to_uid", model.getUserId());
			pushMsg.put("notify_id", model.getNotifyId());
			///添加到push queue
			pushRedis.put(pushMsg.toString());GlobalObject.INFO_LOG.info("PostReplyNotifyWorker add push queue message:" + pushMsg.toString());
			
			///保存到mysql
			postReplyNotifyDao.add(model);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at PostReplyNotifyWorker.execute throw an error.", e);
		}
	}
}