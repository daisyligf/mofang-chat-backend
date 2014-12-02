package com.mofang.chat.backend.init.impl;

import com.mofang.chat.backend.init.AbstractInitializer;
import com.mofang.chat.backend.global.GlobalConfig;
import com.mofang.chat.business.sysconf.SysObject;

/**
 * 
 * @author zhaodx
 *
 */
public class GlobalObjectInitializer extends AbstractInitializer
{
	@Override
	public void load() throws Exception
	{
		SysObject.initRedisMaster(GlobalConfig.REDIS_MASTER_CONFIG_PATH);
		SysObject.initRedisSlave(GlobalConfig.REDIS_SLAVE_CONFIG_PATH);
		SysObject.initGuildSlave(GlobalConfig.GUILD_SLAVE_CONFIG_PATH);
		SysObject.initWriteQueue(GlobalConfig.WRITE_QUEUE_CONFIG_PATH);
		SysObject.initPushQueue(GlobalConfig.PUSH_QUEUE_CONFIG_PATH);
		SysObject.initMysql(GlobalConfig.MYSQL_CONFIG_PATH);
	}
}