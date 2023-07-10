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
import java.util.HashMap;
import java.util.AbstractMap;
import android.util.LongSparseArray;

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
	
	public void markMethodZero(Class<?> zlass,String methodName){
		XposedBridge.hookAllMethods(zlass, methodName, new XC_MethodReplacement(){
				@Override
				protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p1) throws Throwable{
					return null;
				}
			});
	}

	@Override
	protected boolean init(){
		try{
			final ModuleConfigs modConf = ModuleConfigs.getInstance();
			final XC_LoadPackage.LoadPackageParam lpparam = modConf.getLoadPackageParam();
			//阻止UI执行删除
			//public boolean processUpdateArray(ArrayList<TLRPC.Update> updates, 
			//ArrayList<TLRPC.User> usersArr, 
			//ArrayList<TLRPC.Chat> chatsArr, 
			//boolean fromGetDifference, 
			//int date)
			Class<?> zlass = lpparam.classLoader.loadClass("org.telegram.messenger.MessagesController");
			Method wantMethod = zlass.getDeclaredMethod("processUpdateArray",ArrayList.class, ArrayList.class, ArrayList.class, Boolean.TYPE, Integer.TYPE);
			/*
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
			*/
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
			
			zlass = lpparam.classLoader.loadClass("org.telegram.messenger.NotificationsController");
			markMethodZero(zlass,"removeDeletedMessagesFromNotifications");
			
			zlass = lpparam.classLoader.loadClass("org.telegram.messenger.MessagesStorage");
			markMethodZero(zlass,"deleteMessagesByPush");
			/*
			markMethodZero(zlass,"markMessagesAsDeletedByRandoms");
			markMethodZero(zlass,"markMessagesAsDeleted");
			markMethodZero(zlass,"updateDialogsWithDeletedMessages");
			markMethodZero(zlass,"deleteMessages");
			markMethodZero(zlass,"lambda$processUpdateArray$351");
			markMethodZero(zlass,"lambda$deleteMessagesRange$377");
			markMethodZero(zlass,"lambda$deleteMessagesByPush$316");
			*/
			//这个会阻止所有人删除本地消息
			//包括自己的消息（重新进入还原）
			/*
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
				*/
			//lpparam.classLoader.loadClass("com.easy.tgPlus.HookImpl.AntiRetraction$MessageObject");
			/*
			XposedHelpers.findAndHookConstructor(lpparam.classLoader.loadClass("org.telegram.messenger.MessageObject"),new Class<?>[]{
				int.class,
				lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$TL_channelAdminLogEvent"),
				ArrayList.class,
				HashMap.class,
				lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$Chat"),
				int[].class,
				boolean.class
			}, new XC_MethodReplacement(){

					@Override
					protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable{
						Object[] args = param.args;
						int accountNum = args[0];
						Object event = args[1];
						ArrayList messageObjects = (ArrayList)args[2];
						HashMap messagesByDays = (HashMap)args[3];
						Object chat = args[4];
						int[] mid = (int[])args[5];
						boolean addToEnd = args[6];
						return new MessageObject(accountNum,event,messageObjects,messagesByDays,chat,mid,addToEnd);
					}
				});
			*/
			//public MessageObject(int accountNum, TLRPC.Message message, MessageObject replyToMessage, AbstractMap<Long, TLRPC.User> users, AbstractMap<Long, TLRPC.Chat> chats, 
			//LongSparseArray<TLRPC.User> sUsers, LongSparseArray<TLRPC.Chat> sChats, 
			//boolean generateLayout, boolean checkMediaExists, long eid)
			/*
			XposedHelpers.findAndHookConstructor(lpparam.classLoader.loadClass("org.telegram.messenger.MessageObject"),new Class<?>[]{
					int.class,
					lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$Message"),
					lpparam.classLoader.loadClass("org.telegram.messenger.MessageObject"),
					AbstractMap.class,
					AbstractMap.class,
					LongSparseArray.class,
					LongSparseArray.class,
					boolean.class,
					boolean.class,
					long.class
				}, new XC_MethodReplacement(){

					@Override
					protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable{
						Object[] args = param.args;
						int accountNum = args[0];
						Object message = args[1];
						Object replyToMessage = args[2];
						AbstractMap users = (AbstractMap) args[3];
						AbstractMap chats = (AbstractMap) args[4];
						LongSparseArray sUsers = (LongSparseArray) args[5];
						LongSparseArray sChats = (LongSparseArray) args[6];
						boolean generateLayout = args[7];
						boolean checkMediaExists = args[8];
						long eid = args[9];
						return new MessageObject(accountNum,message,replyToMessage,users,chats,sUsers,sChats,generateLayout,checkMediaExists,eid);
					}
				});
			*/
			//添加已删除标签
			Class<?> chatMsgCell = lpparam.classLoader.loadClass("org.telegram.ui.Cells.ChatMessageCell");
			wantMethod = chatMsgCell.getDeclaredMethod("measureTime", lpparam.classLoader.loadClass("org.telegram.messenger.MessageObject"));
			//wantMethod.setAccessible(true);
			XposedBridge.hookMethod(wantMethod, new XC_MethodHook(){
					@Override
					protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable{
						Object msg = param.args[0];
						//deleted变量对消息影响太大
						boolean isdel = XposedHelpers.getBooleanField(msg,"deleted");
						if(isdel == true){
							XposedBridge.log("删除文本绘制成功");
							Object thisMsgCell = param.thisObject;
							String currentTimeString = XposedHelpers.getObjectField(thisMsgCell,"currentTimeString").toString();
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
	
	public class Test extends Object{
		public Test(int accountNum,Object event , ArrayList<MessageObject> messageObjects, HashMap<String, ArrayList<MessageObject>> messagesByDays, Object chat, int[] mid, boolean addToEnd){
			super();
		}
		//public MessageObject(int accountNum, TLRPC.Message message, MessageObject replyToMessage, AbstractMap<Long, TLRPC.User> users, AbstractMap<Long, TLRPC.Chat> chats, LongSparseArray<TLRPC.User> sUsers, LongSparseArray<TLRPC.Chat> sChats, boolean generateLayout, boolean checkMediaExists, long eid)
		public Test(int accountNum, Object message, Object replyToMessage, AbstractMap users, AbstractMap chats, LongSparseArray sUsers, LongSparseArray sChats, boolean generateLayout, boolean checkMediaExists, long eid) {
			super();
		}
	}
	
	public class MessageObject extends Test{
		boolean isdelete = false;
		//public MessageObject(int accountNum, TLRPC.TL_channelAdminLogEvent event, ArrayList<MessageObject> messageObjects, HashMap<String, ArrayList<MessageObject>> messagesByDays, TLRPC.Chat chat, int[] mid, boolean addToEnd) {}
		public MessageObject(int accountNum,Object event , ArrayList<MessageObject> messageObjects, HashMap<String, ArrayList<MessageObject>> messagesByDays, Object chat, int[] mid, boolean addToEnd) {
			super(accountNum,event,messageObjects,messagesByDays,chat,mid,addToEnd);
		}
		public MessageObject(int accountNum, Object message, Object replyToMessage, AbstractMap users, AbstractMap chats, LongSparseArray sUsers, LongSparseArray sChats, boolean generateLayout, boolean checkMediaExists, long eid) {
			super(accountNum,message,replyToMessage,users,chats,sUsers,sChats,generateLayout,checkMediaExists,eid);
		}
	}

}
