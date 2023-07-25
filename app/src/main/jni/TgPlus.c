#include "LSPosedNativeHookAPI.h"
#include <jni.h>
#include <stdio.h>
#include <dlfcn.h>
#include <openssl/ssl.h>
#include "ALog.h"

const char *TAG = "TgPlus";

static HookFunType hook_func = NULL;

const char *(*SSL_get_version2)(SSL *ssl);
const char *(*SSL_get_cipher2)(SSL *ssl);
int (*raw_SSL_do_handshake)(SSL *ssl);
void print_ssl_info(SSL* ssl) {
	#define printf(...) LOGI(__VA_ARGS__)
	
    printf("SSL Connection Information:\n");
    printf("SSL Version: %s\n", SSL_get_version2(ssl));
    printf("Cipher: %s\n", SSL_get_cipher2(ssl));
	/*
    printf("Is Server: %s\n", (ssl->server) ? "Yes" : "No");
    printf("Is Handshake Complete: %s\n", (ssl->s3->hs) ? "Yes" : "No");
    printf("Read Ahead Enabled: %s\n", (ssl->s3->read_ahead) ? "Yes" : "No");
    // Add more information as needed*/
	
	#undef printf
}
int m_SSL_do_handshake(SSL *ssl) {
	print_ssl_info(ssl);
    return raw_SSL_do_handshake(ssl);
}


/*
void print_ssl_info(SSL* ssl) {
    if (ssl == NULL) {
        printf("SSL structure is NULL.\n");
        return;
    }

    printf("SSL Connection Information:\n");

    printf("SSL Version: %s\n", SSL_get_version(ssl));
    // Access SSL_CIPHER directly from SSL struct
    SSL_CIPHER* cipher = ssl->s3->tmp.new_cipher;
    if (cipher) {
        printf("Cipher: %s\n", SSL_CIPHER_get_name(cipher));
    } else {
        printf("Cipher: Not available.\n");
    }

    printf("Is Server: %s\n", (ssl->server) ? "Yes" : "No");
    printf("Is Handshake Complete: %s\n", (ssl->s3->hs) ? "Yes" : "No");
    printf("Read Ahead Enabled: %s\n", (ssl->s3->read_ahead) ? "Yes" : "No");
    // Add more information as needed
}
*/

/*
FILE *(*backup_fopen)(const char *filename, const char *mode);

FILE *fake_fopen(const char *filename, const char *mode) {
    if (strstr(filename, "banned")) return nullptr;
    return backup_fopen(filename, mode);
}
*/
/*
jclass (*backup_FindClass)(JNIEnv *env, const char *name);
jclass fake_FindClass(JNIEnv *env, const char *name)
{
    if(!strcmp(name, "dalvik/system/BaseDexClassLoader"))
        return nullptr;
    return backup_FindClass(env, name);
}
*/
void on_library_loaded(const char *name, void *handle) {
    // hooks on `libtmessages.45.so`
	const char *tarName = "libtmessages.45.so";
	size_t tarLen = strlen(tarName);
	size_t nameLen = strlen(name);
	if(nameLen < tarLen)return;
	const char *simpleName = name + (nameLen - tarLen);
    if (strcmp(simpleName,tarName) == 0) {
		LOGI("目标动态库 %s 已加载",simpleName);
        void *target = dlsym(handle, "SSL_do_handshake");
		SSL_get_version2 = dlsym(handle, "SSL_get_version");
		SSL_get_cipher2 = dlsym(handle, "SSL_get_cipher");
        hook_func(target, (void *) m_SSL_do_handshake, (void **) &raw_SSL_do_handshake);
		LOGI("钩子: SSL_do_handshake");
    }
}

jobject getStaticObjectField(JNIEnv *env,const char *className,const char *name,const char *type){
	jclass javaClass = (*env)->FindClass(env, className);
    if (javaClass == NULL) {
        // 处理错误
        return NULL;
    }

    // 获取静态字段的ID
    jfieldID staticObjectFieldID = (*env)->GetStaticFieldID(env, javaClass, name, type);
    if (staticObjectFieldID == NULL) {
        // 处理错误
        return NULL;
    }

    // 获取静态字段的值
    jstring staticObjectValue = (*env)->GetStaticObjectField(env, javaClass, staticObjectFieldID);

	return staticObjectValue;
}

//extern "C" [[gnu::visibility("default")]] [[gnu::used]]
jint JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv *env = NULL;
    (*jvm)->GetEnv(jvm,(void **)&env, JNI_VERSION_1_6);
	jstring xp_bridge_tag = getStaticObjectField(env,"de/robv/android/xposed/XposedBridge","TAG","Ljava/lang/String;");
	const char* cString = (*env)->GetStringUTFChars(env, xp_bridge_tag, NULL);
	TAG = cString;
    //hook_func((void *)env->functions->FindClass, (void *)fake_FindClass, (void **)&backup_FindClass);
    return JNI_VERSION_1_6;
}

//extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    hook_func = entries->hook_func;
    // system hooks
    //hook_func((void*) fopen, (void*) fake_fopen, (void**) &backup_fopen);
    return on_library_loaded;
}
