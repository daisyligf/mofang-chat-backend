package com.mofang.chat.backend.util.acauto;

import com.mofang.chat.backend.global.GlobalConfig;
import com.mofang.chat.backend.global.GlobalObject;
import com.mofang.chat.backend.util.acauto.AC_auto.AC_auto_Error;

/**
 * 
 * @author zhaodx
 *
 */
public class AcAutoHelper
{
	public static String filter(String input)
	{
		if(null == GlobalObject.AC_AUTO)
    		return input;
		
		StringBuilder out = new StringBuilder();
        StringBuilder tip = new StringBuilder();
        try
        {
	        	AC_auto_Error result = GlobalObject.AC_AUTO.work(input, true, out, tip); 
	        	if(result.has_fatal)
	        	{
	        		GlobalObject.INFO_LOG.info("input:" + input + "    out:" + out.toString());
	        		return null;
	        	}
	        	
	        	return out.toString();
        }
        catch(Exception e)
        {
        		GlobalObject.ERROR_LOG.error("at AcAutoHelper.filter throw an error. parameter:" + input, e);
        		return "";
        }
	}
	
	public static AC_auto load()
	{
		try
		{
			return AC_auto.load_ac_from_db(GlobalConfig.SENSITIVE_WORDS_CONFIG_PATH);
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at AcAutoHelper.load throw an error. ", e);
			return null;
		}
	}
	
	public static void reload()
	{
		try
		{
			AC_auto ac = load();
			if(null != ac)
				GlobalObject.AC_AUTO = ac;
		}
		catch(Exception e)
		{
			GlobalObject.ERROR_LOG.error("at AcAutoHelper.reload throw an error. ", e);
		}
	}
}