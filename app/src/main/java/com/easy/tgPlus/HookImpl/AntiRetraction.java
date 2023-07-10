package com.easy.tgPlus.HookImpl;
import com.easy.tgPlus.HookModule;
import com.easy.tgPlus.ModuleConfigs;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import android.util.Printer;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import com.easy.tgPlus.HookLeader;
import java.util.Iterator;
import java.util.ListIterator;

public class AntiRetraction extends HookModule{

    //模块标签
    public static final String ModuleId = "AntiRetraction";
	//模块名称
	public static final String ModuleName = "防撤回";
	//模块描述
	public static final String ModuleDoc = "对方删除消息时阻止删除并标记消息为已删除";

	public AntiRetraction(){
		super(ModuleId, ModuleName, ModuleDoc);
	}

	//如果需要这个功能可以拿走哦，源代码中需保留作者信息
	//getMessagesStorage().deletePushMessages(j2, arrayList);
	//getMessagesStorage().updateDialogsWithDeletedMessages(j2,
	//j,
	//arrayList,
	//getMessagesStorage().markMessagesAsDeleted(j2, arrayList, false, true, false),
	//false);

	@Override
	protected boolean init(){
		try{
			final ModuleConfigs modConf = ModuleConfigs.getInstance();
			final XC_LoadPackage.LoadPackageParam lpparam = modConf.getLoadPackageParam();
			//阻止UI执行删除
			Class<?> zlass = lpparam.classLoader.loadClass("org.telegram.messenger.MessagesController");
			Method wantMethod = zlass.getDeclaredMethod("processUpdateArray",ArrayList.class, ArrayList.class, ArrayList.class, Boolean.TYPE, Integer.TYPE);
			XposedBridge.hookMethod(wantMethod, new XC_MethodReplacement(){
					@Override
					protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable{
						if (isSwitchOn()){
							return beforeHookedMethod2(param);
						}
						return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}
					
					Class<?> a = lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$TL_updateDeleteMessages");
					Class<?> b = lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$TL_updateDeleteChannelMessages");
					//@Override
					public Object beforeHookedMethod2(MethodHookParam param) throws Throwable{
						
						ListIterator it = ((ArrayList) param.args[0]).listIterator();
						while (it.hasNext()) {
							Object next = it.next();
							if (a.isInstance(next) || b.isInstance(next)) {
								it.remove();
							}
						}
						return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}
				});
			zlass = lpparam.classLoader.loadClass("org.telegram.messenger.NotificationCenter");
			final Class m = zlass;
			XposedBridge.hookAllMethods(zlass, "postNotificationName", new XC_MethodReplacement(){

					@Override
					protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable{
						if(param.args[0] == XposedHelpers.getStaticIntField(m,"messagesDeleted"))
						return null;
						return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}
					
				
			});

			//这个会阻止所有人删除本地消息
			//包括自己的消息（重启还原）
			zlass = lpparam.classLoader.loadClass("org.telegram.messenger.MessagesStorage");
			wantMethod = zlass.getMethod("markMessagesAsDeleted", long.class, ArrayList.class, boolean.class, boolean.class, boolean.class);
			XposedBridge.hookMethod(wantMethod, new XC_MethodReplacement(){
					@Override
					protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable{
						if (isSwitchOn()){
							beforeHookedMethod2(param);
							return null;
						}
						return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}

					//@Override
					public void beforeHookedMethod2(MethodHookParam param){
						//开发中
						long dialogId = param.args[0];
						ArrayList<Integer> messages = (ArrayList<Integer>) param.args[1];
						boolean useQueue = param.args[2];
						boolean forAll = param.args[3];//deleteFiles
						boolean scheduled = param.args[4];
						if (messages == null || messages.size() == 0)return;
						Object msg = messages.get(0);
						XposedBridge.log(String.format("删除消息:从对话(%d)中,messages.length=%d,第一条消息ID:%s,是否为所有人删除:%b", dialogId, messages.size(), msg.toString(), forAll));
					}
				});
			//添加已删除标签
			Class<?> chatMsgCell = lpparam.classLoader.loadClass("org.telegram.ui.Cells.ChatMessageCell");
			wantMethod = chatMsgCell.getDeclaredMethod("measureTime", lpparam.classLoader.loadClass("org.telegram.messenger.MessageObject"));
			//wantMethod.setAccessible(true);
			XposedBridge.hookMethod(wantMethod, new XC_MethodHook(){
					@Override
					protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable{
						Object msg = param.args[0];
						boolean isdel = XposedHelpers.getBooleanField(msg,"deleted");
						if(isdel){
							Object thisMsgCell = param.thisObject;
							String currentTimeString = (String)XposedHelpers.getObjectField(thisMsgCell,"currentTimeString");
							currentTimeString = "已删除 " + currentTimeString;
							XposedHelpers.setObjectField(thisMsgCell,"currentTimeString",currentTimeString);
							Class<?> theme = lpparam.classLoader.loadClass("org.telegram.ui.ActionBar.Theme");
							TextPaint paint = (TextPaint) XposedHelpers.getStaticObjectField(theme,"chat_timePaint");
							assert paint != null;
							int delWidth = (int) Math.ceil(paint.measureText("已删除 "));
							int timeTextWidth = XposedHelpers.getObjectField(thisMsgCell,"timeTextWidth");
							int timeWidth = XposedHelpers.getObjectField(thisMsgCell,"timeWidth");
							XposedHelpers.setObjectField(thisMsgCell,"timeTextWidth",timeTextWidth + delWidth);
							XposedHelpers.setObjectField(thisMsgCell,"timeWidth",timeWidth + delWidth);
						}
					}
				});
			return true;
		}catch (Exception e){
			XposedBridge.log(e);
		}
		return super.init();
	}

}
