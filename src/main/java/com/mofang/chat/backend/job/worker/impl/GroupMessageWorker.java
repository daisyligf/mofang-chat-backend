package com.mofang.chat.backend.job.worker.impl;

import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import com.mofang.chat.backend.global.GlobalConfig;
import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.backend.job.worker.JobWorker;
import com.mofang.chat.backend.util.acauto.AcAutoHelper;
import com.mofang.chat.business.entity.GroupMessage;
import com.mofang.chat.business.model.ChatGroupMessage;
import com.mofang.chat.business.mysql.ChatGroupMessageDao;
import com.mofang.chat.business.mysql.impl.ChatGroupMessageDaoImpl;
import com.mofang.chat.business.redis.GroupMessageRedis;
import com.mofang.chat.business.redis.GroupRedis;
import com.mofang.chat.business.redis.PushQueueRedis;
import com.mofang.chat.business.redis.impl.GroupMessageRedisImpl;
import com.mofang.chat.business.redis.impl.GroupRedisImpl;
import com.mofang.chat.business.redis.impl.PushQueueRedisImpl;
import com.mofang.chat.business.sysconf.common.MessageType;
import com.mofang.chat.business.sysconf.common.PushDataType;

/**
 * 
 * @author zhaodx
 *
 */
public class GroupMessageWorker implements JobWorker
{
	private GroupMessageRedis groupMessageRedis = GroupMessageRedisImpl.getInstance();
	private PushQueueRedis pushRedis = PushQueueRedisImpl.getInstance();
	private ChatGroupMessageDao groupMessageDao = ChatGroupMessageDaoImpl.getInstance();
	private GroupRedis groupUserRedis = GroupRedisImpl.getInstance();
	private JSONObject json;
	
	public GroupMessageWorker(JSONObject json)
	{
		this.json = json;
	}
	
	@Override
	public void execute()
	{
		GroupMessage message = GroupMessage.buildByJson(json);
		if(null == message)
			return;
		
		ChatGroupMessage model = message.toMysqlModel();
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
			groupMessageRedis.save(model, GlobalConfig.REDIS_ROOM_MESSAGE_COUNT);
			
			///根据groupid获取uid集合
			long exceptUserId = message.getFromUserId();
			Map<String, String> uidMap = groupUserRedis.getUserList(message.getGroupId());
			if(null == uidMap || uidMap.size() == 0)
				return;
			
			Iterator<String> iterator = uidMap.keySet().iterator();
			long userId;
			while(iterator.hasNext())
			{
				userId = Long.parseLong(iterator.next());
				if(userId == exceptUserId)
					continue;
				
				///更新群组的用户的未读数(unread_count)
				groupMessageRedis.incrNotifyUnreadCount(userId, message);
			}
			
			///构建push消息
			JSONObject pushMsg = new JSONObject();
			pushMsg.put("push_data_type", PushDataType.GROUP_NOTIFY);
			pushMsg.put("group_id", message.getGroupId());
			pushMsg.put("from_uid", message.getFromUserId());
			///添加到push queue
			pushRedis.put(pushMsg.toString());
			GlobalObject.INFO_LOG.info("GroupMessageWorker add push queue message:" + pushMsg.toString());
			
			///保存到mysql
			groupMessageDao.add(model);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at GroupMessageWorker.execute throw an error.", e);
		}
	}
}
