package com.mofang.chat.backend.job.worker.impl;

import org.json.JSONObject;

import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.backend.job.worker.JobWorker;
import com.mofang.chat.business.model.FeedRecommendNotify;
import com.mofang.chat.business.mysql.FeedRecommendNotifyDao;
import com.mofang.chat.business.mysql.impl.FeedRecommendNotifyDaoImpl;
import com.mofang.chat.business.redis.FeedRecommendNotifyRedis;
import com.mofang.chat.business.redis.PushQueueRedis;
import com.mofang.chat.business.redis.impl.FeedRecommendNotifyRedisImpl;
import com.mofang.chat.business.redis.impl.PushQueueRedisImpl;
import com.mofang.chat.business.sysconf.common.FeedRecommendNotifyStatus;
import com.mofang.chat.business.sysconf.common.PushDataType;

/**
 * 
 * @author zhaodx
 *
 */
public class FeedRecommendNotifyWorker implements JobWorker
{
	private FeedRecommendNotifyRedis feedRecommendNotifyRedis = FeedRecommendNotifyRedisImpl.getInstance();
	private FeedRecommendNotifyDao feedRecommendNotifyDao = FeedRecommendNotifyDaoImpl.getInstance();
	private PushQueueRedis pushRedis = PushQueueRedisImpl.getInstance();
	private JSONObject json;
	
	public FeedRecommendNotifyWorker(JSONObject json)
	{
		this.json = json;
	}

	@Override
	public void execute()
	{
		FeedRecommendNotify model = FeedRecommendNotify.buildByJson(json);
		if(null == model)
			return;
		
		try
		{
			///保存到redis
			model.setStatus(FeedRecommendNotifyStatus.UNREAD);
			feedRecommendNotifyRedis.save(model);
			
			///构建push消息
			/**暂时先不放push_queue
			JSONObject pushMsg = new JSONObject();
			pushMsg.put("push_data_type", PushDataType.FEED_RECOMMEND_NOTIFY);
			pushMsg.put("to_uid", model.getUserId());
			pushMsg.put("notify_id", model.getNotifyId());
			///添加到push queue
			pushRedis.put(pushMsg.toString());
			GlobalObject.INFO_LOG.info("FeedRecommendNotifyWorker add push queue message:" + pushMsg.toString());
			*/
			///保存到mysql
			feedRecommendNotifyDao.add(model);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at FeedRecommendNotifyWorker.execute throw an error.", e);
		}
	}
}