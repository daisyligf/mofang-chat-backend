package com.mofang.chat.backend.job.worker.impl;

import org.json.JSONObject;

import com.mofang.chat.backend.global.GlobalConfig;
import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.backend.job.worker.JobWorker;
import com.mofang.chat.backend.util.acauto.AcAutoHelper;
import com.mofang.chat.business.entity.RoomMessage;
import com.mofang.chat.business.model.ChatRoomMessage;
import com.mofang.chat.business.mysql.ChatRoomMessageDao;
import com.mofang.chat.business.mysql.impl.ChatRoomMessageDaoImpl;
import com.mofang.chat.business.redis.PushQueueRedis;
import com.mofang.chat.business.redis.RoomMessageRedis;
import com.mofang.chat.business.redis.RoomRedis;
import com.mofang.chat.business.redis.impl.PushQueueRedisImpl;
import com.mofang.chat.business.redis.impl.RoomMessageRedisImpl;
import com.mofang.chat.business.redis.impl.RoomRedisImpl;
import com.mofang.chat.business.sysconf.common.MessageType;
import com.mofang.chat.business.sysconf.common.PushDataType;

/**
 * 
 * @author zhaodx
 *
 */
public class RoomMessageWorker implements JobWorker
{
	private RoomMessageRedis roomMessageRedis = RoomMessageRedisImpl.getInstance();
	private RoomRedis roomRedis = RoomRedisImpl.getInstance();
	private PushQueueRedis pushRedis = PushQueueRedisImpl.getInstance();
	private ChatRoomMessageDao roomMessageDao = ChatRoomMessageDaoImpl.getInstance();
	private JSONObject json;
	
	public RoomMessageWorker(JSONObject json)
	{
		this.json = json;
	}
	
	@Override
	public void execute()
	{
		RoomMessage message = RoomMessage.buildByJson(json);
		if(null == message)
			return;
		
		ChatRoomMessage model = message.toMysqlModel();
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
			roomMessageRedis.save(model, GlobalConfig.REDIS_ROOM_MESSAGE_COUNT);
			
			///更新房间最后一条消息的发布时间
			roomRedis.setLastTimestamp(message.getRoomId(), message.getTimeStamp());
			
			///构建push消息
			JSONObject pushMsg = new JSONObject();
			pushMsg.put("push_data_type", PushDataType.ROOM_NOTIFY);
			pushMsg.put("rid", message.getRoomId());
			pushMsg.put("from_uid", message.getFromUserId());
			///添加到push queue
			pushRedis.put(pushMsg.toString());
			GlobalObject.INFO_LOG.info("RoomMessageWorker add push queue message:" + pushMsg.toString());
			
			///保存到mysql
			roomMessageDao.add(model);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at RoomMessageWorker.execute throw an error.", e);
		}
	}
}