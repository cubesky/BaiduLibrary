import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.Semaphore;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.DefaultHttpParams;

public class BaiduUtils {
	Semaphore tlock=new Semaphore(1);
	HttpClient client;
	Cookie [] cookies;
	String token;
	String user;
	String pass;
	String codestring="";
	String verifycode="";
	public BaiduUtils(String user,String pass){
		client=new HttpClient();
		DefaultHttpParams.getDefaultParams().setParameter("http.protocol.cookie-policy", CookiePolicy.BROWSER_COMPATIBILITY);
		try {
			this.user=URLEncoder.encode(user,"utf-8");
			this.pass=URLEncoder.encode(pass,"utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	private void getToken() throws BaiduException{
		try{
			tlock.acquireUninterruptibly();
			GetMethod gm1=new GetMethod("https://passport.baidu.com/v2/api/?getapi&tpl=tb&apiver=v3&tt="+System.currentTimeMillis()+"&class=login&logintype=dialogLogin&callback=bd__cbs__sbw");
			gm1.addRequestHeader("Connection", "keep-alive");
			int code=SendCookieRequest(gm1);
			if(code==200){
				GetMethod gm2=new GetMethod("https://passport.baidu.com/v2/api/?getapi&tpl=tb&apiver=v3&tt="+System.currentTimeMillis()+"&class=login&logintype=dialogLogin&callback=bd__cbs__sbw");
				gm1.addRequestHeader("Connection", "keep-alive");
				SendCookieRequest(gm2);
				String result=getResponse(gm2);
				result=result.substring(result.indexOf("token")+10);
				result=result.substring(0,result.indexOf("\""));
				tlock.release();
				token = result;
			}else{
				tlock.release();
				throw new BaiduException();
			}
		}catch(Exception e){
			throw new BaiduException();
		}
	}
	private NameValuePair[] getLoginParameters(){
		NameValuePair[] data={
				new NameValuePair("password",pass),
				new NameValuePair("apiver","v3"),
				new NameValuePair("callback","parent.bd__pcbs__sbw"),
				new NameValuePair("charset","UTF-8"),
				new NameValuePair("codestring",codestring),
				new NameValuePair("isPhone","false"),
				new NameValuePair("logintype","bascilogin"),
				new NameValuePair("mem_pass","on"),
				new NameValuePair("password",pass),
				new NameValuePair("ppui_logintime","8888"),
				new NameValuePair("quick_user","0"),
				new NameValuePair("safeflg","0"),
				new NameValuePair("splogin","rate"),
				new NameValuePair("staticpage","http://tieba.baidu.com/tb/static-common/html/pass/v3Jump.html"),
				new NameValuePair("token",token),
				new NameValuePair("tpl","tb"),
				new NameValuePair("tt",String.valueOf(System.currentTimeMillis())),
				new NameValuePair("u","http://tieba.baidu.com/"),
				new NameValuePair("username",user),
				new NameValuePair("verifycode",verifycode)
		};
		return data;
	}
	private NameValuePair[] getDeleteParameters(String tieba,String tid) throws BaiduException{
		try{
			NameValuePair[] data={
					new NameValuePair("commit_fr","pb"),
					new NameValuePair("ie","utf-8"),
					new NameValuePair("kw",URLEncoder.encode(tieba,"utf-8")),
					new NameValuePair("fid",getFid(tieba)),
					new NameValuePair("tbs",getTbs(tid)),
					new NameValuePair("tid",tid)
			};
			return data;
		}catch(Exception e){
			throw new BaiduException();
		}
	}
	public void loginwithCookie() throws BaiduException{
		String str=FileReadUtils.readFileByChars(System.getProperty("user.dir")+System.getProperty("file.separator")+"cookie.conf");
		GetMethod get = new GetMethod("http://www.baidu.com");
		get.addRequestHeader("Connection", "keep-alive");
		tlock.acquireUninterruptibly();
		try {
			SendCookieRequestwithnosave(get);
		} catch (Exception e) {
			e.printStackTrace();
		}
		tlock.release();
		String[] sstr=str.split("\n");
		cookies=client.getState().getCookies();
		HttpState stats=new HttpState();
		cookies[0].setName(sstr[0]);
		cookies[0].setValue(sstr[1]);
		stats.addCookie(cookies[0]);
		client.setState(stats);
	}
	public void saveCookie(String str){
		str=str.substring(0,str.length()-1);
		FileWriteUtils.getFileFromBytes(str.getBytes(), System.getProperty("user.dir")+System.getProperty("file.separator")+"cookie.conf");
	}
	public boolean login() throws BaiduException {
		boolean isLogin=false;
		try{
			PostMethod pm1=new PostMethod("https://passport.baidu.com/v2/api/?login");
			getToken();
			tlock.acquireUninterruptibly();
			pm1.addParameters(getLoginParameters());
			pm1.addRequestHeader("Connection", "keep-alive");
			int httpcode=SendCookieRequest(pm1);
			if(httpcode==200){
				String result=getResponse(pm1);
				if(result.indexOf("error=257")>=0){
					result=result.substring(result.indexOf("&codestring=")+"&codestring=".length());
					result=result.substring(0,result.indexOf("&username="));
					codestring=result;
					System.out.println("NeedCode");
				}
				tlock.release();
				isLogin=checkLogin();
			}else{
				tlock.release();
				throw new BaiduException();
			}
		}catch(Exception e){
			tlock.release();
			throw new BaiduException();
		}
		return isLogin;
	}
	public String getFid(String tieba) throws BaiduException{
		try {
			tlock.acquireUninterruptibly();
			GetMethod get=new GetMethod("http://tieba.baidu.com/f?kw="+URLEncoder.encode(tieba,"gb2312"));
			get.addRequestHeader("Connection", "keep-alive");
			SendCookieRequest(get);
			String result=getResponse(get);
			result=result.substring(result.indexOf("\"forum_id\":")+"\"forum_id\":".length());
			result=result.substring(0, result.indexOf(","));
			tlock.release();
			return result;
		} catch (Exception e) {
			tlock.release();
			throw new BaiduException();
		}
	}
	public String getTbs(String tid) throws BaiduException{
		try {
			tlock.acquireUninterruptibly();
			GetMethod get=new GetMethod("http://tieba.baidu.com/p/"+tid);
			get.addRequestHeader("Connection", "keep-alive");
			SendCookieRequest(get);
			String result=getResponse(get);
			result=result.substring(result.indexOf("var PageData = {        \"tbs\": \"")+"var PageData = {        \"tbs\": \"".length());
			result=result.substring(0, result.indexOf("\""));
			tlock.release();
			return result;
		} catch (Exception e) {
			tlock.release();
			throw new BaiduException();
		}
	}
	public String getTbsTieba(String tieba) throws BaiduException{
		try{
			tlock.acquireUninterruptibly();
			GetMethod get=new GetMethod("http://tieba.baidu.com/bawu2/platform/index?word="+URLEncoder.encode(tieba,"gb2312")+"&ie=utf-8");
			get.addRequestHeader("Connection", "keep-alive");
			SendCookieRequest(get);
			String result=getResponse(get);
			result=result.substring(result.indexOf("\"tbs\":\"")+"\"tbs\":\"".length());
			result=result.substring(0, result.indexOf("\""));
			tlock.release();
			return result;
		}catch(Exception e){
			tlock.release();
			throw new BaiduException();
		}
	}
	public byte[] getVCode() throws BaiduException{
		GetMethod get=new GetMethod("https://passport.baidu.com/cgi-bin/genimage?"+codestring);
		get.addRequestHeader("Connection", "keep-alive");
		try{
			tlock.acquireUninterruptibly();
			SendCookieRequest(get);
			tlock.release();
			return get.getResponseBody();
		}catch(Exception e){
			tlock.release();
			throw new BaiduException();
		}
	}
	public void delete(String tieba,String tid) throws BaiduException{
		try{
			PostMethod delete=new PostMethod("http://tieba.baidu.com/f/commit/thread/delete");
			delete.addRequestHeader("Connection", "keep-alive");
			NameValuePair[] data=getDeleteParameters(tieba,tid);
			delete.setRequestBody(data);
			delete.setRequestHeader("Referer", "http://tieba.baidu.com/p/"+tid);
			delete.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			tlock.acquireUninterruptibly();
			SendCookieRequest(delete);
			System.out.println("Run Delete!");
			tlock.release();
		}catch(Exception e){
			tlock.release();
			throw new BaiduException();
		}
	}
	private NameValuePair[] getBlockIdParameter(String tieba,String user,String day,String reason,String pid) throws UnsupportedEncodingException, BaiduException{
		if(pid!=null){
			NameValuePair[] r={
					new NameValuePair("day",day),
					new NameValuePair("fid",getFid(tieba)),
					new NameValuePair("ie","gbk"),
					new NameValuePair("reason",reason),
					new NameValuePair("tbs",getTbsTieba(tieba)),
					new NameValuePair("pid[]",pid),
					new NameValuePair("user_name[]",user)
			};
			return r;
		}else{
			NameValuePair[] r={
					new NameValuePair("day",day),
					new NameValuePair("fid",getFid(tieba)),
					new NameValuePair("ie","gbk"),
					new NameValuePair("reason",URLEncoder.encode(reason,"gb2312")),
					new NameValuePair("tbs",getTbsTieba(tieba)),
					new NameValuePair("user_name[]",user),
					new NameValuePair("anonymous","0")
			};
			return r;
		}
		
	}
	public void blockid(String tieba,String user,String day,String reason,String pid) throws BaiduException{
		try{
			PostMethod pblock=new PostMethod("http://tieba.baidu.com/pmc/blockid");
			NameValuePair[] data=getBlockIdParameter(tieba,user,"1",reason,pid);
			pblock.setRequestBody(data);
			pblock.setRequestHeader("Referer", "http://tieba.baidu.com/bawu2/platform/listMember?word="+URLEncoder.encode(tieba, "gb2312"));
			pblock.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			pblock.setRequestHeader("Connection", "keep-alive");
			tlock.acquireUninterruptibly();
			int code=SendCookieRequest(pblock);
			if(code==200){
				String r=getResponse(pblock);
				if(r.startsWith("{\"errno\":0")){
					System.out.println("Blocked User :  "+user);
					tlock.release();
				}else{
					System.out.println(r);
					tlock.release();
					throw new BaiduException();
				}
			}else{
				tlock.release();
				throw new BaiduException();
			}
		}catch(Exception e){
			tlock.release();
			e.printStackTrace();
			throw new BaiduException();
		}
	}
	private boolean checkLogin() throws BaiduException{
		GetMethod get = new GetMethod("http://www.baidu.com");
		get.addRequestHeader("Connection", "keep-alive");
		boolean res = false;
		try {
			tlock.acquireUninterruptibly();
			SendCookieRequest(get);
			String content = getResponse(get);
			if(content.indexOf("登录")<0){
				res = true;
			}
		} catch (Exception e){
			tlock.release();
			throw new BaiduException();
		}
		tlock.release();
		return res;
	}
	private String getResponse(HttpMethod method) throws BaiduException{
		try {
			InputStream inputStream = method.getResponseBodyAsStream();   
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));   
			StringBuffer stringBuffer = new StringBuffer();   
			String str= "";   
			while((str = br.readLine()) != null){   
				stringBuffer.append(str);   
			}
			return stringBuffer.toString();
		} catch (IOException e) {
			throw new BaiduException();
		}   
	}
	private int SendCookieRequest(HttpMethod hm) throws Exception{
		int httpcodes=client.executeMethod(hm);
		client.getState().getCookies();
		cookies = client.getState().getCookies();
		if(cookies !=null && cookies.length>0){

			String cook=cookies[0].getValue();
			String c=cookies[0].getName();
			for (int i = 1; i < cookies.length; i++) {
				cook += "; " + cookies[i].getName() + "=" + cookies[i].getValue();
			}
			cook+=";";
			saveCookie(c+"\n"+cook);
			cookies[0].setValue(cook);
			HttpState state = new HttpState();
			state.addCookie(cookies[0]);
			client.setState(state);
		}  
		return httpcodes;
	}
	private int SendCookieRequestwithnosave(HttpMethod hm) throws Exception{
		int httpcodes=client.executeMethod(hm);
		client.getState().getCookies();
		cookies = client.getState().getCookies();
		if(cookies !=null && cookies.length>0){

			String cook=cookies[0].getValue();
			for (int i = 1; i < cookies.length; i++) {
				cook += "; " + cookies[i].getName() + "=" + cookies[i].getValue();
			}
			cook+=";";
			cookies[0].setValue(cook);
			HttpState state = new HttpState();
			state.addCookie(cookies[0]);
			client.setState(state);
		}  
		return httpcodes;
	}
}
