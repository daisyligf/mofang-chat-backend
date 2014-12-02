package com.mofang.chat.backend.job.worker.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mofang.chat.backend.global.GlobalConfig;
import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.backend.job.worker.JobWorker;
import com.mofang.chat.business.model.RoomUser;
import com.mofang.chat.business.mysql.RoomUserDao;
import com.mofang.chat.business.mysql.impl.RoomUserDaoImpl;
import com.mofang.chat.business.redis.RoomRedis;
import com.mofang.chat.business.redis.impl.RoomRedisImpl;

/**
 * 
 * @author zhaodx
 *
 */
public class RoomUserWorker implements JobWorker
{
	private RoomUserDao roomUserDao = RoomUserDaoImpl.getInstance();
	private RoomRedis roomRedis = RoomRedisImpl.getInstance();
	private JSONObject json;
	
	public RoomUserWorker(JSONObject json)
	{
		this.json = json;
	}
	
	@Override
	public void execute()
	{
		try
		{
			long userId = json.optLong("uid", 0L);
			JSONArray array = json.getJSONArray("rid_list");
			if(0 == userId || null == array || array.length() == 0)
				return;
			
			///构建最新订阅的房间集合
			Set<Integer> roomSet = new HashSet<Integer>();
			for(int i=0; i<array.length(); i++)
				roomSet.add(array.getInt(i));
			
			///获取已经订阅的房间集合
			List<RoomUser> oriRoomList = roomUserDao.getRooms(userId);
			Set<Integer> oriRoomSet = new HashSet<Integer>();
			if(null != oriRoomList && oriRoomList .size() > 0)
			{
				for(RoomUser roomUser : oriRoomList)
					oriRoomSet.add(roomUser.getRoomId());
			}
			
			///获取新订阅房间集合 和 已经订阅的房间集合的交集, 定义为订阅交集
			Set<Integer> retainRoomSet = new HashSet<Integer>();
			retainRoomSet.addAll(oriRoomSet);
			retainRoomSet.retainAll(roomSet);
			
			///获取 已经订阅的房间集合 和 订阅交集的差集, 该差集就是用户已经退订的房间集合, 定义为退订差集
			Set<Integer> oriRemoveRoomSet = new HashSet<Integer>();
			oriRemoveRoomSet.addAll(oriRoomSet);
			oriRemoveRoomSet.removeAll(retainRoomSet);
			
			///获取 新订阅房间集合 和 订阅交集的差集, 该差集就是用户新增订阅的房间集合, 定义为新增差集
			Set<Integer> newRemoveRoomSet = new HashSet<Integer>();
			newRemoveRoomSet.addAll(roomSet);
			newRemoveRoomSet.removeAll(retainRoomSet);
			
			///遍历退订差集, 删除已退订的房间记录
			if(oriRemoveRoomSet.size() > 0)
			{
				Iterator<Integer> iterator = oriRemoveRoomSet.iterator();
				Integer roomId;
				while(iterator.hasNext())
				{
					roomId = iterator.next();
					///从redis中删除
					roomRedis.unsubscribeRoom(roomId, userId);
					
					///从mysql中删除
					roomUserDao.delete(roomId, userId);
				}
			}
			
			///遍历新增差集, 添加新订阅的房间记录
			if(newRemoveRoomSet.size() > 0)
			{
				Iterator<Integer> iterator = newRemoveRoomSet.iterator();
				Integer roomId;
				while(iterator.hasNext())
				{
					roomId = iterator.next();
					///添加到redis中
					long maxCount = roomRedis.getSubscribeUserCount(roomId);
					if(maxCount < GlobalConfig.REDIS_ROOM_USER_COUNT)
						roomRedis.subscribeRoom(roomId, userId, 0);
					
					///添加到mysql中
					RoomUser roomUser = new RoomUser();
					roomUser.setRoomId(roomId);
					roomUser.setUserId(userId);
					roomUser.setMsgCount(0);
					roomUserDao.add(roomUser);
				}
			}
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at RoomUserWorker.execute throw an error.", e);
		}
	}
}