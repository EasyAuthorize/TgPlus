package com.easy.tgPlus.HookImpl;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.SparseArray;
import android.widget.Toast;
import com.easy.tgPlus.BuildConfig;
import com.easy.tgPlus.HookModule;
import com.easy.tgPlus.ModuleConfigs;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
	
	//当前版本最高flag在第28位
	//啊deleted，你比top_Topic多1倍
	//我觉得这种手法需要在readme里面声明一下危险性
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
								//dialogId=0为私聊,需要查UID
								dialogId = 0;
								ArrayList<Integer> delMsg = (ArrayList<Integer>) XposedHelpers.getObjectField(next,"messages");
								SparseArray<Object> msgs = (SparseArray<Object>) XposedHelpers.getObjectField(param.thisObject,"dialogMessagesByIds");
								for(int id : delMsg){
									//Object msgObj = XposedHelpers.callMethod(msgs,"get",new Class<?>[]{int.class},new Object[]{id});
									Object msgObj = msgs.get(id);
									if(msgObj == null){
										//这里如果查找不到说明这条消息不是正在显示(猜的
										XposedBridge.log("udm :delMsg size = "+delMsg.size()+" find msgObj = null");
										break;
									}else{
										Object mes = XposedHelpers.getObjectField(msgObj,"messageOwner");
										XposedHelpers.setIntField(mes,"flags", XposedHelpers.getIntField(mes,"flags") | FLAG_DELETED);
										//toast(msgObj);
									}
								}
								markMessagesDeleted(param.thisObject,dialogId,delMsg);
								it.remove();
							}else if(udcm.isInstance(next)){
								//频道消息id为负数(手动取反
								dialogId = -XposedHelpers.getLongField(next,"channel_id");
								ArrayList<Integer> delMsg = (ArrayList<Integer>) XposedHelpers.getObjectField(next,"messages");
								//LongSparseArray<ArrayList<Object>> 
								Object laMsgs = XposedHelpers.getObjectField(param.thisObject,"dialogMessage");
								ArrayList<Object> objs = (ArrayList<Object>) XposedHelpers.callMethod(laMsgs,"get",new Class<?>[]{int.class},new Object[]{dialogId});
								if(objs == null){
									XposedBridge.log("udcm :delMsg size = "+delMsg.size()+" find msgObj = null");
								} else{
									for(final Object msgObj : objs){
										if(delMsg.contains(XposedHelpers.callMethod(msgObj,"getId"))){
											Object mes = XposedHelpers.getObjectField(msgObj,"messageOwner");
											XposedHelpers.setIntField(mes,"flags", XposedHelpers.getIntField(mes,"flags") | FLAG_DELETED);
											//toast(msgObj);
										}
									}
								}
								
								markMessagesDeleted(param.thisObject,dialogId,delMsg);
								//我们迫切的需要在这里刷新对话框的msg!!!
								//否则只能在view复用重新绑定数据或者退出重进才能发现消息已删除!!!
								it.remove();
							}
						}
						return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}
					
					public void markMessagesDeleted(Object thisMessagesController, long dialogId,ArrayList<Integer> delMsg){
						Object db = XposedHelpers.callMethod(XposedHelpers.callMethod(thisMessagesController,"getMessagesStorage"),"getDatabase");
						String query = "SELECT data,mid,uid "+
							"FROM messages_v2 "+
							//如果dialogId = 0，则需要查询UID(作为UPDATE查询的条件)，并且where子句需要添加is_channel = 0
							//虽然只有mid就能查到，但是官方源码是这么写的，那就照做
							"WHERE " + (dialogId==0?"is_channel":"uid") + " = "+dialogId+" AND mid IN ("+TextUtils.join(",",delMsg)+");";

						String update = "UPDATE messages_v2 SET data = ? WHERE uid = ? AND mid = ?;";
						Object cursor = XposedHelpers.callMethod(db,"queryFinalized",new Class<?>[]{String.class,Object[].class},new Object[]{query,new Object[]{}});
						Object state = XposedHelpers.callMethod(db,"executeFast",new Class<?>[]{String.class},new Object[]{update});

						while((boolean)XposedHelpers.callMethod(cursor,"next")){
							//查询原始data
							Object data = XposedHelpers.callMethod(cursor,"byteBufferValue",new Class<?>[]{int.class},new Object[]{0});
							//别问我为什么是int,官方自己就用的ArrayList<Integer>
							int mid = XposedHelpers.callMethod(cursor,"intValue",new Class<?>[]{int.class},new Object[]{1});
							dialogId = XposedHelpers.callMethod(cursor,"longValue",new Class<?>[]{int.class},new Object[]{2});

							XposedHelpers.callMethod(data,"position",new Class<?>[]{int.class},new Object[]{4});
							int flags = XposedHelpers.callMethod(data,"readInt32",new Class<?>[]{boolean.class},new Object[]{true});
							flags |= FLAG_DELETED;
							XposedHelpers.callMethod(data,"position",new Class<?>[]{int.class},new Object[]{4});
							XposedHelpers.callMethod(data,"writeInt32",new Class<?>[]{int.class},new Object[]{flags});
							XposedHelpers.callMethod(data,"position",new Class<?>[]{int.class},new Object[]{0});

							//重新查询(UPDATE)
							XposedHelpers.callMethod(state,"requery");
							XposedHelpers.callMethod(state,"bindByteBuffer",new Class<?>[]{int.class,data.getClass()},new Object[]{1,data});
							XposedHelpers.callMethod(state,"bindLong",new Class<?>[]{int.class,long.class},new Object[]{2,dialogId});
							XposedHelpers.callMethod(state,"bindInteger",new Class<?>[]{int.class,int.class},new Object[]{3,mid});
							XposedHelpers.callMethod(state,"step");
							
							XposedHelpers.callMethod(data,"reuse");
							if(BuildConfig.DEBUG)
								XposedBridge.log("flags = "+flags);
						}
						XposedHelpers.callMethod(cursor,"dispose");
						XposedHelpers.callMethod(state,"dispose");
						if(BuildConfig.DEBUG)
							XposedBridge.log("标记消息 :dialogId | channel_id = "+ dialogId + " delMsg size = "+delMsg.size());
					}
					
					public void toast(final Object msgObj){
						//像这样形成一个三角形的函数调用我们称之为"三角函数"
						try{
							XposedHelpers.callStaticMethod(lpparam.classLoader.loadClass("org.telegram.messenger.AndroidUtilities"), "runOnUIThread", new Class[]{Runnable.class}, new Object[]{
									new Runnable(){
										@Override
										public void run(){
											try{
												CharSequence content = (CharSequence) XposedHelpers.getObjectField(msgObj, "messageText");
												Toast.makeText(modConf.getContext(), "防撤回:" + content, Toast.LENGTH_SHORT).show();
											}catch (Exception e){}
										}
									}
								});
						}catch (ClassNotFoundException e){}
					}
					
				});
			
			//zlass = lpparam.classLoader.loadClass("org.telegram.messenger.MessagesStorage");
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
						if((flags & FLAG_DELETED) != 0){
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
