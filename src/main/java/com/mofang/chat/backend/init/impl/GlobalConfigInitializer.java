package com.mofang.chat.backend.init.impl;

import java.io.IOException;

import com.mofang.chat.backend.init.AbstractInitializer;
import com.mofang.chat.backend.global.GlobalConfig;
import com.mofang.framework.util.IniParser;

/**
 * 
 * @author zhaodx
 *
 */
public class GlobalConfigInitializer extends AbstractInitializer
{
	private String configPath;
	
	public GlobalConfigInitializer(String configPath)
	{
		this.configPath = configPath;
	}
	
	@Override
	public void load() throws IOException 
	{
		IniParser config = new IniParser(configPath);
		GlobalConfig.REDIS_ROOM_MESSAGE_COUNT = config.getInt("common", "redis_room_message_count");
		GlobalConfig.REDIS_PRIVATE_MESSAGE_COUNT = config.getInt("common", "redis_private_message_count");
		GlobalConfig.REDIS_ROOM_USER_COUNT = config.getInt("common", "redis_room_user_count");
		
		GlobalConfig.MYSQL_CONFIG_PATH = config.get("conf", "mysql_config_path");
		GlobalConfig.REDIS_MASTER_CONFIG_PATH = config.get("conf", "redis_master_config_path");
		GlobalConfig.REDIS_SLAVE_CONFIG_PATH = config.get("conf", "redis_slave_config_path");
		GlobalConfig.GUILD_SLAVE_CONFIG_PATH = config.get("conf", "guild_slave_config_path");
		GlobalConfig.LOG4J_CONFIG_PATH = config.get("conf", "log4j_config_path");
		GlobalConfig.PUSH_QUEUE_CONFIG_PATH = config.get("conf", "push_queue_config_path");
		GlobalConfig.WRITE_QUEUE_CONFIG_PATH = config.get("conf", "write_queue_config_path");
		GlobalConfig.SENSITIVE_WORDS_CONFIG_PATH = config.get("conf", "sensitive_words_config_path");
		
		GlobalConfig.AC_AUTO_TASK_TIME = config.get("cron", "ac_auto_task_time");
		GlobalConfig.ROOM_USER_TASK_TIME = config.get("cron", "room_user_task_time");
	}
}