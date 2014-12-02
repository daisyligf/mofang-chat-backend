package com.mofang.chat.backend.util.acauto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*; 

import com.mofang.chat.backend.global.GlobalObject;

public class AC_auto
{
	int max_len = 1;
	final int byte_size = 256;
	byte[] insert_buf = new byte[4096];
	char replace_char = '*';
	
	// 驱动程序名
	static String driver = "com.mysql.jdbc.Driver";
	// URL指向要访问的数据库名
	static String mysql = "";
	// MySQL配置时的用户名
	static String user = "";
	// Java连接MySQL配置时的密码
	static String password = "";
	static String keyword_table = "";
	static String message_table = "";

	// insert时，先插入这个Map中，再在gen_trie_tree的时候往数组里填
	Map<String, AC_auto_Element> ele_map = new HashMap<String, AC_auto_Element>();
	// 提示词Map
	Map<Integer, String> tips_map = new HashMap<Integer, String>();
	// 解决全角vs半角、简体vs繁体的类对象
	Encoding encoding = new Encoding();

	// 词串的个数（非重）
	int tot;
	// trie树本身，第一个index是byte的256个可能性，第二个index是tot
	int[][] c;
	// 失败指针
	int[] fail;
	// 当前节点如果是敏感词节点，则记录此敏感词的长度(按byte为单位)；如果不是，则为0
	int[] id;
	// 当前节点如果是敏感词节点，表示其重要性；否则为0.
	int[] l;
	// 当前节点的敏感词提示语id
	int[] t;

	public class AC_auto_Error
	{
		public int error_num;
		public boolean has_fatal;

		public AC_auto_Error()
		{
			error_num = 0;
			has_fatal = false;
		}
	}

    public static AC_auto load_ac_from_db(String config_name) throws Exception
    {
    	AC_auto ac_tmp = null;
    	load_config(config_name);
    	try 
    	{
	    	// 加载驱动程序
	    	Class.forName(driver);
	    	// 连续数据库
	    	Connection conn = DriverManager.getConnection(mysql, user, password);
	    	if(!conn.isClosed())
	    		GlobalObject.INFO_LOG.info("Succeeded connecting to the Database!");
	
	    	// statement用来执行SQL语句
	    	Statement statement = conn.createStatement();
	
	    	// 加载敏感词
	    	String sql = "select * from "+keyword_table;

	    	GlobalObject.INFO_LOG.info("loading data from: "+keyword_table);  
	    	ResultSet rs = statement.executeQuery(sql);  
	    	
	    	// 初始化一个ac自动机，计算真实需要的node数量
	    	GlobalObject.INFO_LOG.info("constructing tmp AC_auto");  
	    	ac_tmp = new AC_auto(); 
	    	String keyword = null;  
	    	int level = 0;
	    	int mess_id = 0; 

	    	while(rs.next()) 
	    	{  
	    		level = rs.getInt("level");
	    		mess_id = rs.getInt("message_id");
	    		keyword = rs.getString("keyword");
	    		ac_tmp.insert(keyword, level, mess_id);
	    	}  
	    	rs.close();  

	    	GlobalObject.INFO_LOG.info("constructing trie tree"); 
	        ac_tmp.gen_trie_tree();  	
	        
	        // 加载提示词
	        GlobalObject.INFO_LOG.info("loading tips from: "+message_table); 
	    	
	    	if (!message_table.isEmpty()) 
	    	{
		        sql = "select * from "+message_table;  
		    	rs = statement.executeQuery(sql);  
		    	int id = 0;
		    	String message = null;
		    	while(rs.next())
		    	{  
		    		id = rs.getInt("id");
		    		message = rs.getString("message");
		    		ac_tmp.insert_mess(id, message);
		    	}  
		    	rs.close();  
	    	}
	    	conn.close();   
	    } 
    	catch(ClassNotFoundException e) 
	    {   
	    	throw e;
	    } catch(SQLException e) 
	    {   
	    	throw e;   
	    } catch(Exception e)
	    {   
	    	throw e;   
	    }   
    	
        // 创建一个新的ac自动机，缩减数组大小。reload可用此方式。
    	GlobalObject.INFO_LOG.info("constructing final AC_auto");  
		AC_auto ac = new AC_auto(ac_tmp);
		GlobalObject.INFO_LOG.info("AC_auto constructed, ready to work");  
		
		return ac;
    }

	/**
     * 敏感词查找，可设置是否将敏感词替换为*，并且支持返回提示词
     * @param in String, need_replace boolean, out StringBuilder, tips StringBuilder
     * @return AC_auto_Error
     */
	public AC_auto_Error work(String in, boolean need_replace, StringBuilder out, StringBuilder tips) throws UnsupportedEncodingException
	{
		// 解决全角vs半角、简体vs繁体
		String s = Encoding.ToDBC(in);
		s = encoding.ToJianti(s);
		// s = Encoding.ReplaceSpecialChar(s);
		
		byte[] s1 = s.getBytes("unicode");
		int len = s1.length;
		int s_len = in.length();
		int i, u = 0, j;
		AC_auto_Error ret = new AC_auto_Error();

		// 记录in中每个character是否需要被替换
		boolean[] replace_array = null;
		if (need_replace) {
			replace_array = new boolean[s_len];
			for (i = 0; i < s_len; i++) {
				replace_array[i] = false;
			}
		}

		// 因为我们采用utf-16编码，所以前两字节是endian的说明，需要忽略
		for (i = 2; i < len; i++) 
		{
			int k = s1[i] & 0xFF;
			u = c[k][u];
			j = u;

			boolean first_time = true;
			while (j != 0) 
			{
				if (id[j] != 0) 
				{
					if (l[j] >= WordLevel.FORBIDDEN) { // 禁发词
						ret.has_fatal = true;
					} else if (l[j] == WordLevel.TIPS) { // 提示词
						tips.delete(0, tips.length());
						tips.append(tips_map.get((Integer) t[j]));
					}
					ret.error_num += 1;

					// 第一次检测到敏感词（最长的那个），并且请求需要用*代替敏感词，
					// 并且该敏感词的级别高于仅仅设置提示词的时候，记录在replace_array中。
					// 注意，采用utf-16编码，所以前两字节是endian的说明，并且utf-16编码都是2字节编码
					if (first_time && need_replace && l[j]>WordLevel.TIPS) {
						for (int z = (i - 3) / 2, c = 0; c < id[j] / 2; c++, z--) {
							replace_array[z] = true;
						}
					}
					first_time = false;
				}
				j = fail[j];
			}
		}

		if (need_replace) 
		{
			for (i = 0; i < s_len; i++) {
				if (replace_array[i]) {
					out.append(replace_char);
				} else {
					out.append(in.charAt(i));
				}
			}
		}

		return ret;
	}

	private class AC_auto_Element
	{
		int level;
		int tips_id;
		String keyword;

		AC_auto_Element(int l, int id, String k)
		{
			level = l;
			tips_id = id;
			keyword = k;
		}
	}

	private class WordLevel
	{
		//public final static int NORMAL = 0;
		public final static int TIPS = 1;
		//public final static int REPLACE = 2;
		public final static int FORBIDDEN = 3;
	}
    
    private static boolean load_config(String config_name) throws Exception
    {
    	Properties configurations = new Properties();
        try 
        {
        	//String path = AC_auto.class.getClassLoader().getSystemResource("").getPath();
        	//FileInputStream fis = new FileInputStream(new File(config_name));
        	//InputStream inputStream = AC_auto.class.getClassLoader().getResourceAsStream(config_name);
        	InputStream inputStream = new FileInputStream(new File(config_name));
			configurations.load(inputStream);
			mysql = configurations.getProperty("mysql");
			user = configurations.getProperty("user");
			password = configurations.getProperty("password");
			keyword_table = configurations.getProperty("keyword_table");
			message_table = configurations.getProperty("message_table");
        }
	    catch(IOException e)
	    {
	    	throw e;
	    }

        return true;
    }

	// 初始化ac自动机
	private AC_auto() {
	}

	/**
     * 用于trim
     * @param src AC_auto.
     * @return AC_auto
     */
	private AC_auto(AC_auto src) 
	{
		System.out.println("trie tree node num: " + src.tot);
		tot = src.tot;
		c = new int[byte_size][tot];
		fail = new int[tot];
		id = new int[tot];
		l = new int[tot];
		t = new int[tot];

		for (int i = 0; i < byte_size; i++) {
			for (int j = 0; j < tot; j++) {
				c[i][j] = src.c[i][j];
			}
		}

		for (int i = 0; i < tot; i++) {
			fail[i] = src.fail[i];
			id[i] = src.id[i];
			l[i] = src.l[i];
			t[i] = src.t[i];
		}

		Iterator<Map.Entry<Integer, String>> iter = src.tips_map.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry<Integer, String> entry = (Map.Entry<Integer, String>) iter.next();
			Integer key = entry.getKey();
			String val = entry.getValue();
			tips_map.put(key, val);
		}
	}

	/**
     * 用于插入提示词
     * @param id int, mess String.
     * @return void
     */
	private void insert_mess(int id, String mess)
	{
		tips_map.put(id, mess);
	}

	/**
     * 用于插入敏感词，只是放在map中，后续调用gen_trie_tree时再生成trie树
     * @param s String, level int, tips_id int.
     * @return void
     */
	private void insert(String s, int level, int tips_id) throws UnsupportedEncodingException
	{
		// 解决全角vs半角、简体vs繁体
		s = Encoding.ToDBC(s);
		s = encoding.ToJianti(s);
		// s = Encoding.ReplaceSpecialChar(s);

		AC_auto_Element e = new AC_auto_Element(level, tips_id, s);
		if (!ele_map.containsKey(s)) {
			ele_map.put(s, e);
			byte[] b = s.getBytes("unicode");
			max_len += b.length;
		}
	}

	/**
     * 生成trie树
     * @param void
     * @return void
     */
	private void gen_trie_tree() throws UnsupportedEncodingException
	{
		// 申请空间
		init(max_len);
		System.out.println("trie tree max node num: " + max_len);

		// 构造基本树结构
		Iterator<Map.Entry<String, AC_auto_Element>> iter = ele_map.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry<String, AC_auto_Element> entry = (Map.Entry<String, AC_auto_Element>) iter.next();
			AC_auto_Element ele = (AC_auto_Element) entry.getValue();
			insert_internal(ele.keyword, ele.level, ele.tips_id);
		}
		ele_map = null;

		// 构造失败指针
		Queue<Integer> tmp_queue = new LinkedList<Integer>();
		int u = 0, e, i;
		for (i = 0; i < byte_size; i++)
			if (c[i][u] != 0)
				tmp_queue.add(c[i][u]);
		
		while (!tmp_queue.isEmpty()) {
			u = tmp_queue.poll();
			for (i = 0; i < byte_size; i++) {
				if (c[i][u] != 0) {
					e = c[i][u];
					fail[e] = c[i][fail[u]];
					tmp_queue.add(e);
				} else {
					c[i][u] = c[i][fail[u]];
				}
			}
		}
	}

	private void init(int len)
	{
		tot = 0;
		c = new int[byte_size][len];
		fail = new int[len];
		id = new int[len];
		l = new int[len];
		t = new int[len];

		new_node();
	}

	private int new_node()
	{
		int i;
		for (i = 0; i < byte_size; i++)
			c[i][tot] = 0;
		fail[tot] = id[tot] = 0;
		l[tot] = 0;
		t[tot] = 0;
		return tot++;
	}

	private void insert_internal(String s, int level, int tip_id) throws UnsupportedEncodingException
	{
		insert_buf = s.getBytes("unicode");
		int now = 0;
		int len = insert_buf.length, i;
		for (i = 2; i < len; i++) {
			int k = insert_buf[i] & 0xFF;
			if (c[k][now] == 0)
				c[k][now] = new_node();
			now = c[k][now];
		}

		id[now] = insert_buf.length - 2;
		l[now] = level;
		t[now] = tip_id;
	}
}