package com.mofang.chat.backend.global;

import org.apache.log4j.Logger;

import com.mofang.chat.backend.util.acauto.AC_auto;

/**
 * 
 * @author zhaodx
 *
 */
public class GlobalObject
{
	/**
	 * Global Info Logger Instance 
	 */
	public final static Logger INFO_LOG = Logger.getLogger("backend.info");
	
	/**
	 * Global Error Logger Instance
	 */
	public final static Logger ERROR_LOG = Logger.getLogger("backend.error");
	
	/**
	 * Global AC_auto Instance
	 */
	public static AC_auto AC_AUTO;
}