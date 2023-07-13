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
import android.util.SparseArray;
import android.widget.Toast;
import java.util.Arrays;
import de.robv.android.xposed.BuildConfig;
import android.text.TextUtils;

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
	
	//当前版本最高flag28位
	public static final int FLAG_DELETED = 1 << 28;

	//如果需要这个功能可以拿走哦，源代码中需保留作者信息
	//getMessagesStorage().deletePushMessages(j2, arrayList);
	//getMessagesStorage().updateDialogsWithDeletedMessages(j2,
	//j,
	//arrayList,
	//getMessagesStorage().markMessagesAsDeleted(j2, arrayList, false, true, false),
	//false);
	
	//TL_messages_deleteMessages.readParams(AbstractSerializedData stream, boolean exception)
	
	//updateWidgets(dialogsIds);
	//dialogsToUpdate.get(did);
	
	//这个好像是插入消息用的
	//XposedHelpers.callMethod(param.thisObject,"updateInterfaceWithMessages",new Class<?>[]{long.class,ArrayList.class,boolean.class},new Object[]{dialogId,objs,false})
	
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
			
			XposedBridge.hookMethod(wantMethod, new XC_MethodReplacement(){
					@Override
					protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable{
						if (isSwitchOn()){
							return beforeHookedMethod2(param);
						}
						return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}
					
					Class<?> udm = lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$TL_updateDeleteMessages");
					Class<?> udcm = lpparam.classLoader.loadClass("org.telegram.tgnet.TLRPC$TL_updateDeleteChannelMessages");
					//@Override
					public Object beforeHookedMethod2(final MethodHookParam param) throws Throwable{
						ListIterator it = ((ArrayList) param.args[0]).listIterator();
						long dialogId;
						while (it.hasNext()) {
							Object next = it.next();
							if (udm.isInstance(next)) {
								//dialogId=0为私聊则需要查UID
								dialogId = 0;
								//mid ids
								ArrayList<Integer> delMsg = (ArrayList<Integer>) XposedHelpers.getObjectField(next,"messages");
								SparseArray<Object> msgs = (SparseArray<Object>) XposedHelpers.getObjectField(param.thisObject,"dialogMessagesByIds");
								for(int id : delMsg){
									//Object msgObj = XposedHelpers.callMethod(msgs,"get",new Class<?>[]{int.class},new Object[]{id});
									Object msgObj = msgs.get(id);
									if(msgObj == null){
										//这里如果查找不到说明这条消息不是正在显示(猜的
										XposedBridge.log("udm :delMsg size = "+delMsg.size()+" find msgObj = null");
										break;
									}									
									Object mes = XposedHelpers.getObjectField(msgObj,"messageOwner");
									XposedHelpers.setIntField(mes,"flags", XposedHelpers.getIntField(mes,"flags") | FLAG_DELETED);
								}
								Object db = XposedHelpers.callMethod(XposedHelpers.callMethod(param.thisObject,"getMessagesStorage"),"getDatabase");
								
								it.remove();
								XposedBridge.log("udm :delMsg size = "+delMsg.size());
							}else if(udcm.isInstance(next)){
								dialogId = -XposedHelpers.getLongField(next,"channel_id");
								ArrayList<Integer> delMsg = (ArrayList<Integer>) XposedHelpers.getObjectField(next,"messages");
								//LongSparseArray<ArrayList<Object>> 
								Object laMsgs = XposedHelpers.getObjectField(param.thisObject,"dialogMessage");
								ArrayList<Object> objs = (ArrayList<Object>) XposedHelpers.callMethod(laMsgs,"get",new Class<?>[]{int.class},new Object[]{dialogId});
								for(final Object msgObj : objs){
									if(delMsg.contains(XposedHelpers.callMethod(msgObj,"getId"))){
										Object mes = XposedHelpers.getObjectField(msgObj,"messageOwner");
										XposedHelpers.setIntField(mes,"flags", XposedHelpers.getIntField(mes,"flags") | FLAG_DELETED);
										XposedHelpers.callStaticMethod(lpparam.classLoader.loadClass("org.telegram.messenger.AndroidUtilities"), "runOnUIThread", new Class[]{Runnable.class}, new Object[]{
												new Runnable(){
													@Override
													public void run(){
														try{
															CharSequence content = (CharSequence) XposedHelpers.getObjectField(msgObj,"messageText");
															Toast.makeText(modConf.getContext(),"防撤回:"+content,Toast.LENGTH_SHORT).show();
														}catch (Exception e){}
													}
												}
											});
									}
								}
								Object db = XposedHelpers.callMethod(XposedHelpers.callMethod(param.thisObject,"getMessagesStorage"),"getDatabase");
								String update = "SELECT data "+
												"FROM messages_v2 "+
												"WHERE uid = "+dialogId+" AND mid IN ("+TextUtils.join(",",delMsg)+");";
								Object cursor = XposedHelpers.callMethod(db,"queryFinalized",new Class<?>[]{String.class,Object[].class},new Object[]{update,new Object[]{}});
								
								while((boolean)XposedHelpers.callMethod(cursor,"next")){
									//查询原始data
									//好像可以从已有的Message对象调用序列化方法直接构造，懒得搞
									Object data = XposedHelpers.callMethod(cursor,"byteBufferValue",new Class<?>[]{int.class},new Object[]{0});
									
									XposedHelpers.callMethod(data,"position",new Class<?>[]{int.class},new Object[]{4});
									int flags = XposedHelpers.callMethod(data,"readInt32",new Class<?>[]{boolean.class},new Object[]{true});
									flags |= FLAG_DELETED;
									XposedHelpers.callMethod(data,"position",new Class<?>[]{int.class},new Object[]{4});
									XposedHelpers.callMethod(data,"writeInt32",new Class<?>[]{int.class},new Object[]{flags});
									XposedHelpers.callMethod(data,"position",new Class<?>[]{int.class},new Object[]{0});
									//写入数据库
									XposedBridge.log("flags = "+flags);
									XposedHelpers.callMethod(data,"reuse");
								}
								XposedHelpers.callMethod(cursor,"dispose");
								XposedBridge.log("SQL执行完毕");
								it.remove();
								XposedBridge.log("udcm :did | channel_id = "+ dialogId + "delMsg size = "+delMsg.size());
							}
						}
						XposedHelpers.callStaticMethod(lpparam.classLoader.loadClass("org.telegram.messenger.AndroidUtilities"), "runOnUIThread", new Class[]{Runnable.class}, new Object[]{
								new Runnable(){
									@Override
									public void run(){
										
									}
								}
							});
						return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}
					
					public void markMessagesDeleted(int dialogId,ArrayList<Integer> delMsg){
						
					}
					
				});
			
			zlass = lpparam.classLoader.loadClass("org.telegram.messenger.MessagesStorage");
			/*
			markMethodZero(zlass,"markMessagesAsDeleted");
			markMethodZero(zlass,"updateDialogsWithDeletedMessages");
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
			
			//添加已删除标签
			Class<?> chatMsgCell = lpparam.classLoader.loadClass("org.telegram.ui.Cells.ChatMessageCell");
			wantMethod = chatMsgCell.getDeclaredMethod("measureTime", lpparam.classLoader.loadClass("org.telegram.messenger.MessageObject"));
			//wantMethod.setAccessible(true);
			XposedBridge.hookMethod(wantMethod, new XC_MethodHook(){
					
					@Override
					protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable{
						Object msgObj = param.args[0];
						Object msg = XposedHelpers.getObjectField(msgObj,"messageOwner");
						//deleted变量对消息有影响
						int flags = XposedHelpers.getIntField(msg,"flags");
						//10000000000000000000000000000
						if((flags & 0x10000000) != 0){
							if(BuildConfig.DEBUG)
								XposedBridge.log("删除标记绘制成功");
							Object thisMsgCell = param.thisObject;
							CharSequence currentTimeString = (CharSequence) XposedHelpers.getObjectField(thisMsgCell,"currentTimeString");
							currentTimeString = "已删除 " + currentTimeString;
							XposedHelpers.setObjectField(thisMsgCell,"currentTimeString",currentTimeString);
							Class<?> theme = lpparam.classLoader.loadClass("org.telegram.ui.ActionBar.Theme");
							TextPaint paint = (TextPaint) XposedHelpers.getStaticObjectField(theme,"chat_timePaint");
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
