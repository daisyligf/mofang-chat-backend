package com.mofang.chat.backend.cron.task;

import java.util.List;

import com.mofang.chat.backend.global.GlobalConfig;
import com.mofang.chat.backend.global.GlobalObject;
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
public class RoomUserTask implements Runnable
{
	private RoomUserDao roomUserDao = RoomUserDaoImpl.getInstance();
	private RoomRedis roomUserRedis = RoomRedisImpl.getInstance();
	
	@Override
	public void run() 
	{
		try
		{
			///获取所有房间ID
			List<Integer> roomIds = roomUserDao.getAllRooms();
			if(null == roomIds)
				return;
			
			List<RoomUser> users = null;
			for(Integer roomId : roomIds)
			{
				///获取房间订阅用户(上限 REDIS_ROOM_USER_COUNT)
				users = roomUserDao.getUsersOrderByCount(roomId, GlobalConfig.REDIS_ROOM_USER_COUNT);
				if(null == users || users.size() == 0)
					continue;
				
				///清空redis中房间的订阅用户列表
				roomUserRedis.clearSubscribeRoom(roomId);
				
				///填充房间的订阅用户列表
				for(RoomUser user : users)
					roomUserRedis.subscribeRoom(roomId, user.getUserId(), user.getMsgCount());
			}
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at RoomUserTask.run throw an error. ", e);
		}
	}
}