# 百度访问

## 示例
BaiduUtils baidu=new BaiduUtils("用户名","密码"); //创建BaiduUtils对象，目前对中文用户名支持不是很好
baidu.loginwithCookie(); //使用cookie登陆，需要在您软件的相同目录下放置cookie.conf
baidu.login(); //使用用户名、密码登陆
baidu.delete("贴吧名", "帖子的tid"); //删帖
baidu.blockid("贴吧名", "用户", "天数", "原因", "用户的Pid"); //封禁用户

## 第三方库
 Apache Commons HttpClient
 
## 注意
 本代码原为在贴吧遭遇爆吧时自动处理而设计。

## 协议
 本代码使用GPLv3协议发布，请使用代码时注意协议要求
