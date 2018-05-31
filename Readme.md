### 简介
本项目提供了了人脸识别的两个开箱即用的组件: FaceDetectView和FaceEngine. 前者主要用于从安卓设备
自带摄像头或者外接UVC摄像头中获取相片流.后者用于对图片进行处理, 提取相关信息等, 是个通用的接口, 本
项目还提供了arcsoft的离线人脸识别引擎对该接口的实现以及百度在线人脸api对该引擎的对接.

##### 使用
在项目根目录下的build.gradle里面加入maven中央仓库, 如下所示

    allprojects {
        repositories {
            // 其它仓库, 如google()等
            mavenCentral()
        }
    }

在要使用该库的模块的build.gradle中加入如下依赖(使用arcsoft离线识别引擎):

    dependencies {
        // 其它依赖
        implementation 'com.github.charleslzq:arcsoft-binding:1.0.1-RC1'
    }

或者使用百度在线人脸api


    dependencies {
        // 其它依赖
        implementation 'com.github.charleslzq:baidu-binding:1.0.1-RC1'
    }

另外还需要加入以下代码避免报错:

    android {
        //其它配置, 如sdk版本等

        packagingOptions {
            exclude 'META-INF/rxjava.properties'
        }
    }

### FaceDetectView
该组件有其生命周期, 一般跟包含它的activity或者fragment同步. 在xml中有如下配置参数

    1. showTrackRect 布尔型,是否在View中显示检测到头像的方框, 默认为true
    2. rectColor 颜色, 显示方框边框的颜色, 默认为Color.RED
    3. rectWidth demension, 显示方框边框的宽度, 默认为1f
    4. sampleInterval integer, 取样间隔, 每隔该时间发送一次相片取样, 单位毫秒, 默认为500

获取相片流在创建时调用其onPreview接口即可, java需实现CameraPreview.FrameConsumer接口, 如下所示:

    faceDetectView.onPreview(new CameraPreview.FrameConsumer() {
                @Override
                public void accept(@NotNull CameraPreview.PreviewFrame previewFrame) {
                    // 处理收到的数据
                }
            });

该接口有多个重载版本, 可指定超时时间(默认为2秒)或者指定处理逻辑在哪个线程执行(默认为rxjava的
computation线程) . 收到的数据类型为CameraPreview.PreviewFrame, 各字段含义如下:

    1. source 相片来源, 为camera的标识符
    2. size 相片大小, Resolution类型, 可获取长\宽\面积\长宽比等数据
    3. image byte数组, 图像原始数据, 为nv21编码
    4. rotation 倾斜角度
    5. sequence 该相片的序号, 可能为空

该组件可获取安卓内置摄像头(底层使用Fotoapparat)以及外接UVC摄像头(底层使用UVCCamera), 启动时按UVC
摄像头\前置摄像头\后置摄像头的顺序进行检测, 并打开第一个可用的摄像头. 此后在接入新的外接UVC摄像头时自动
切换到该摄像头. 可通过给operatorSourceSelector赋值来重新选择摄像头来源(内置或者UVC), 然后对摄像头
来源的operatorSelector赋值来选择使用的摄像头. 1.0.1版本开始默认使用最低分辨率.

### FaceEngine
该接口是人脸离线识别的通用接口, 并带有三个泛型参数, 含义按顺序如下:

    1. 所处理的包含图像数据的类
    2. 包含人元信息的类
    3. 包含人脸元信息的类

并提供了两个基础接口

    1. detect 用于从图片中检测人脸
    2. search 用于根据图片搜索对应的用户

离线引擎还包括两个泛型参数,分别用于表示人脸相似度的类和存储人脸数据.

目前本项目提供了arcsoft的实现ArcSoftFaceEngine, 通过服务的方式
提供人脸识别的基础功能, 包括人脸识别与比较, 检测性别与年龄等等. arcsoft模块默认提供了两种服务:

    1. LocalArcSoftEngineService 所有数据都保存在本地, 不跟服务器端进行交互
    2. WebSocketArcSoftEngineService 当尝试把新数据写入时, 它将通过websocket会向服务器端发送数据而不是保存到本地. 同时接受通过服务器端通过websocket推送过来的数据.

这些服务除了最基础的接口外, 还提供了年龄和性别检测的接口

百度模块则提供了一个服务 BaiduFaceEngineService. 该服务也提供了直接调用百度api的接口, 各接口和[百度人脸接口](http://ai.baidu.com/docs#/Face-Java-SDK/d126963d)一一对应.

本项目同时提供了一些工具类:

    1. ImageUtils, 其toBitmap方法可将包含NV21编码的图像原始数据转换成Bitmap, toEncodedBytes方法用于将图像编码,使其符合百度的要求
    2. Nv21ImageUtils, 其toNv21Bytes方法提供相反的转换,即bitmap转换成Nv21编码的byte数组

### ArcSoft的配置
sample和sample-java项目assets目录下有一个sample.arcsoft.keys, 将其拷到项目的assets文件夹
下并更名为arcsoft.keys, 填上从arcsoft官网获取到的app id, key等即可. 具体参数的配置在sample/sample-java资源
values目录的ArcSoftSetting.xml里面, 参照它以及arcsoft官网的说明进行配置即可.

### 百度引擎的配置
因为调用百度的api需要使用在百度云服务上申请的token等信息, 为了保证token的安全, 百度建议将token放在服务器端,
所以百度模块需要配合服务器端库[baifu-face-server](https://github.com/charleslzq/baidu-face-server)使用. 可以通过string属性Baidu_Base或者BaiduFaceEngineService
的setUrl方法配置服务器地址. 另外百度客户端的接口底层使用kotlin的协程实现, 在java中调用时需要使用CoroutineSupport
的blockingGet方法, 如下所示:

    CoroutineSupport.blockingGet(baiduFaceEngineService.listGroup(0, 100))

