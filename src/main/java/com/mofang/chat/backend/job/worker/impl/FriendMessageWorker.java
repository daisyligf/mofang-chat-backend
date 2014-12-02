package com.mofang.chat.backend.job.worker.impl;

import org.json.JSONObject;

import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.backend.job.worker.JobWorker;
import com.mofang.chat.backend.util.acauto.AcAutoHelper;
import com.mofang.chat.business.entity.FriendMessage;
import com.mofang.chat.business.model.ChatFriendMessage;
import com.mofang.chat.business.mysql.ChatFriendMessageDao;
import com.mofang.chat.business.mysql.impl.ChatFriendMessageDaoImpl;
import com.mofang.chat.business.redis.PushQueueRedis;
import com.mofang.chat.business.redis.UserRedis;
import com.mofang.chat.business.redis.impl.PushQueueRedisImpl;
import com.mofang.chat.business.redis.impl.UserRedisImpl;
import com.mofang.chat.business.sysconf.common.PushDataType;

/**
 * 
 * @author zhaodx
 *
 */
public class FriendMessageWorker implements JobWorker
{
	private UserRedis userRedis = UserRedisImpl.getInstance();
	private PushQueueRedis pushRedis = PushQueueRedisImpl.getInstance();
	private ChatFriendMessageDao friendMessageDao = ChatFriendMessageDaoImpl.getInstance();
	private JSONObject json;
	
	public FriendMessageWorker(JSONObject json)
	{
		this.json = json;
	}
	
	@Override
	public void execute()
	{
		FriendMessage message = FriendMessage.buildByJson(json);
		if(null == message)
			return;
		
		ChatFriendMessage model = message.toMysqlModel();
		if(null == model)
			return;
		
		try
		{
			///过滤敏感词
			String filterContent = AcAutoHelper.filter(model.getContent());
			if(null == filterContent)
				return;
			
			model.setContent(filterContent);
			
			///更新好友申请/处理结果的通知列表
			userRedis.setFriendNotify(message);
			
			///构建push消息
			JSONObject pushMsg = new JSONObject();
			pushMsg.put("push_data_type", PushDataType.FRIEND_NOTIFY);
			pushMsg.put("to_uid", message.getToUserId());
			pushMsg.put("from_uid", message.getFromUserId());
			///添加到push queue中
			pushRedis.put(pushMsg.toString());GlobalObject.INFO_LOG.info("FriendMessageWorker add push queue message:" + pushMsg.toString());
			
			///保存到mysql
			friendMessageDao.add(model);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at FriendMessageWorker.execute throw an error.", e);
		}
	}
}