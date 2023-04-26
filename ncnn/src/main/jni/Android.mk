LOCAL_PATH := $(call my-dir)

# change this folder path to yours
NCNN_INSTALL_PATH := ${LOCAL_PATH}/ncnn-android-vulkan-lib

include $(CLEAR_VARS)
LOCAL_MODULE := ncnn
LOCAL_SRC_FILES := $(NCNN_INSTALL_PATH)/$(TARGET_ARCH_ABI)/libncnn.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := glslang
LOCAL_SRC_FILES := $(NCNN_INSTALL_PATH)/$(TARGET_ARCH_ABI)/libglslang.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := SPIRV
LOCAL_SRC_FILES := $(NCNN_INSTALL_PATH)/$(TARGET_ARCH_ABI)/libSPIRV.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := OGLCompiler
LOCAL_SRC_FILES := $(NCNN_INSTALL_PATH)/$(TARGET_ARCH_ABI)/libOGLCompiler.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := OSDependent
LOCAL_SRC_FILES := $(NCNN_INSTALL_PATH)/$(TARGET_ARCH_ABI)/libOSDependent.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := combined_jni
LOCAL_SRC_FILES := combined_jni.cpp

LOCAL_C_INCLUDES := $(NCNN_INSTALL_PATH)/include $(NCNN_INSTALL_PATH)/include/ncnn

LOCAL_STATIC_LIBRARIES := ncnn glslang SPIRV OGLCompiler OSDependent

LOCAL_CFLAGS := -O2 -fvisibility=hidden -fomit-frame-pointer -fstrict-aliasing -ffunction-sections -fdata-sections -ffast-math
LOCAL_CPPFLAGS := -O2 -fvisibility=hidden -fvisibility-inlines-hidden -fomit-frame-pointer -fstrict-aliasing -ffunction-sections -fdata-sections -ffast-math
LOCAL_LDFLAGS += -Wl,--gc-sections

LOCAL_CFLAGS += -fopenmp
LOCAL_CPPFLAGS += -fopenmp
LOCAL_LDFLAGS += -static-openmp -fopenmp

LOCAL_LDLIBS := -lz -llog -ljnigraphics -lvulkan -landroid

include $(BUILD_SHARED_LIBRARY)
