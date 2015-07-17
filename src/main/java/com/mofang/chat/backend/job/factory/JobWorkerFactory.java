package com.mofang.chat.backend.job.factory;

import org.json.JSONObject;

import com.mofang.chat.backend.job.worker.JobWorker;
import com.mofang.chat.backend.job.worker.impl.FeedRecommendNotifyWorker;
import com.mofang.chat.backend.job.worker.impl.FriendMessageWorker;
import com.mofang.chat.backend.job.worker.impl.GroupMessageWorker;
import com.mofang.chat.backend.job.worker.impl.PostReplyNotifyWorker;
import com.mofang.chat.backend.job.worker.impl.PrivateMessageWorker;
import com.mofang.chat.backend.job.worker.impl.RoomMessageWorker;
import com.mofang.chat.backend.job.worker.impl.RoomUserWorker;
import com.mofang.chat.backend.job.worker.impl.SysMessageNotifyWorker;
import com.mofang.chat.business.sysconf.common.WriteDataType;

/**
 * 
 * @author zhaodx
 *
 */
public class JobWorkerFactory
{
	public static JobWorker getInstance(int dataType, JSONObject json)
	{
		JobWorker worker = null;
		switch(dataType)
		{
			case WriteDataType.ROOM_MESSAGE:
				worker = new RoomMessageWorker(json);
				break;
			case WriteDataType.PRIVATE_MESSAGE:
				worker = new PrivateMessageWorker(json);
				break;
			case WriteDataType.FRIEND_MESSAGE:
				worker = new FriendMessageWorker(json);
				break;
			case WriteDataType.USER_SUBSCRIBE_ROOMS:
				worker = new RoomUserWorker(json);
				break;
			case WriteDataType.GROUP_MESSAGE:
				worker = new GroupMessageWorker(json);
				break;
			case WriteDataType.POST_REPLY_NOTIFY:
				worker = new PostReplyNotifyWorker(json);
				break;
			case WriteDataType.SYS_MESSAGE_NOTIFY:
				worker = new SysMessageNotifyWorker(json);
				break;
			case WriteDataType.FEED_RECOMMEND:
				worker = new FeedRecommendNotifyWorker(json);
				break;
		}
		return worker;
	}
}