package com.mofang.chat.backend.init.impl;

import org.apache.log4j.PropertyConfigurator;

import com.mofang.chat.backend.init.AbstractInitializer;
import com.mofang.chat.backend.init.Initializer;
import com.mofang.chat.backend.util.acauto.AcAutoHelper;
import com.mofang.chat.backend.global.GlobalConfig;
import com.mofang.chat.backend.global.GlobalObject;

/**
 * 
 * @author zhaodx
 *
 */
public class MainInitializer extends AbstractInitializer
{
	private String configPath;
	
	public MainInitializer(String configPath)
	{
		this.configPath = configPath;
	}
	
	@Override
	public void load() throws Exception
	{
		Initializer globalConf = new GlobalConfigInitializer(configPath);
		globalConf.init();
		
		PropertyConfigurator.configure(GlobalConfig.LOG4J_CONFIG_PATH);
		
		Initializer globalObject = new GlobalObjectInitializer();
		globalObject.init();
		
		GlobalObject.AC_AUTO = AcAutoHelper.load();
	}
}