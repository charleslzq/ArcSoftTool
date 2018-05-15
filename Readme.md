### 简介
本项目提供了了人脸识别的两个开箱即用的组件: FaceDetectView和FaceEngine. 前者主要用于从安卓设备
自带摄像头或者外接UVC摄像头中获取相片流.后者用于对图片进行处理, 提取相关信息等, 是个通用的接口, 本
项目已经提供了arcsoft的人脸识别引擎对该接口的实现, 并计划加入百度人脸识别引擎的实现.

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
来源的operatorSelector赋值来选择使用的摄像头.

### FaceEngine
该接口是人脸离线识别的通用接口, 并带有五个泛型参数, 含义按顺序如下:

    1. 所处理的包含图像数据的类
    2. 包含人元信息的类
    3. 包含人脸元信息的类
    4. 表示人脸相似度的类
    5. 存储人脸数据的类

因为百度人脸识别是在线进行的, 所以该类之后可能稍作调整. 目前本项目提供了arcsoft的实现ArcSoftFaceEngine, 通过服务的方式
提供人脸识别的基础功能, 包括人脸识别与比较, 检测性别与年龄等等. arcsoft模块默认提供了两种服务:

    1. LocalArcSoftEngineService 所有数据都保存在本地, 不跟服务器端进行交互
    2. WebSocketArcSoftEngineService 当尝试把新数据写入时, 它将通过websocket会向服务器端发送数据而不是保存到本地. 同时接受通过服务器端通过websocket推送过来的数据.

本项目同时提供了一些工具类:

    1. ImageUtils, 其toBitmap方法可将包含NV21编码的图像原始数据转换成Bitmap
    2. Nv21ImageUtils, 其toNv21Bytes方法提供相反的转换,即bitmap转换成Nv21编码的byte数组

### ArcSoft的配置
项目根目录下有一个sample.local.properties, 将其中Arcsoft开头的配置项拷到使用该库项目根目录下的
local.properties中, 填上从arcsoft官网获取到的app id, key等即可. 具体参数的配置在sample资源
values目录的ArcSoftSetting.xml里面, 参照它以及arcsoft官网的说明进行配置即可.

