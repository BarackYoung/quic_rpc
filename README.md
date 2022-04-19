# quic_rpc

#### 介绍
基于QUIC和netty实现的RPC框架，序列化使用google protobuf，实现语言为java.

#### 软件架构
1.web服务:使用springboot,可正常对外提供REST API  

2.RPC服务：底层使用netty框架，并使用了netty实现的QUIC(基于UDP的可靠传输协议)  

3.注册中心：使用eureka

#### 安装教程

1. 直接从仓库下载代码即可使用。
2. 代码中只有network包是必须的，其余为demo演示使用

#### 使用说明

1. 使用IDEA加载下载好的代码
2. 修改配置文件  
rpc.service.port = rpc端口号  
rpc.application.name = rpc appId  
rpc.eureka.url = eureka地址
3. 使用接口定义服务,定义请求数据类型和返回数据类型，service端和client端各保存一份
4. 服务server端实现接口，实现类添加注解@RpcServer
5. client端接口上添加注解@RpcClient("appId")
6. client端使用ClientProxy.getService(Class interface， LoadBalanceFactory.get())获取服务，并直接调用方法
7. 支持异步调用，异步调用的回调类要实现RpcRespondHandler接口，并在Client端接口异步方法添加注解@Asynchronous("异步回调实现类的全限定名")

