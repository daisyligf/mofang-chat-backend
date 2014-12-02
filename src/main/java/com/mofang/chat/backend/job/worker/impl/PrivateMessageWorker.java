package com.mofang.chat.backend.job.worker.impl;

import org.json.JSONObject;

import com.mofang.chat.backend.global.GlobalConfig;
import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.backend.job.worker.JobWorker;
import com.mofang.chat.backend.util.acauto.AcAutoHelper;
import com.mofang.chat.business.entity.PrivateMessage;
import com.mofang.chat.business.model.ChatPrivateMessage;
import com.mofang.chat.business.mysql.ChatPrivateMessageDao;
import com.mofang.chat.business.mysql.impl.ChatPrivateMessageDaoImpl;
import com.mofang.chat.business.redis.PrivateMessageRedis;
import com.mofang.chat.business.redis.PushQueueRedis;
import com.mofang.chat.business.redis.UserRedis;
import com.mofang.chat.business.redis.impl.PrivateMessageRedisImpl;
import com.mofang.chat.business.redis.impl.PushQueueRedisImpl;
import com.mofang.chat.business.redis.impl.UserRedisImpl;
import com.mofang.chat.business.sysconf.common.MessageType;
import com.mofang.chat.business.sysconf.common.PushDataType;

/**
 * 
 * @author zhaodx
 *
 */
public class PrivateMessageWorker implements JobWorker
{
	private PrivateMessageRedis privateMessageRedis = PrivateMessageRedisImpl.getInstance();
	private UserRedis userRedis = UserRedisImpl.getInstance();
	private PushQueueRedis pushRedis = PushQueueRedisImpl.getInstance();
	private ChatPrivateMessageDao privateMessageDao = ChatPrivateMessageDaoImpl.getInstance();
	private JSONObject json;
	
	public PrivateMessageWorker(JSONObject json)
	{
		this.json = json;
	}

	@Override
	public void execute()
	{
		PrivateMessage message = PrivateMessage.buildByJson(json);
		if(null == message)
			return;
		
		ChatPrivateMessage model = message.toMysqlModel();
		if(null == model)
			return;
		
		try
		{
			///过滤敏感词
			if(model.getMessageType() == MessageType.TEXT)
			{
				String filterContent = AcAutoHelper.filter(model.getContent());
				if(null == filterContent)
					return;
				
				model.setContent(filterContent);
			}
			
			///保存到redis
			privateMessageRedis.save(model, GlobalConfig.REDIS_PRIVATE_MESSAGE_COUNT);
			
			///更新私聊的用户的未读数(unread_count)
			userRedis.incrUnreadCount(message);
			
			///构建push消息
			JSONObject pushMsg = new JSONObject();
			pushMsg.put("push_data_type", PushDataType.PRIVATE_NOTIFY);
			pushMsg.put("to_uid", message.getToUserId());
			pushMsg.put("from_uid", message.getFromUserId());
			///添加到push queue
			pushRedis.put(pushMsg.toString());GlobalObject.INFO_LOG.info("PrivateMessageWorker add push queue message:" + pushMsg.toString());
			
			///保存到mysql
			privateMessageDao.add(model);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at PrivateMessageWorker.execute throw an error.", e);
		}
	}
}