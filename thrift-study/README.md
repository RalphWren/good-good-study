# Thrift 学习

## 安装

1. mac 安装 0.9.3 版本

`brew install https://gist.githubusercontent.com/chrislusf/8b4e7c19551ba220232f037b43c0eaf3/raw/01465b867b8ef9af7c7c3fa830c83666c825122d/thrift.rb`

## 学习

### hello-world

1. 运行 maven 插件，生成 java 代码。
2. 分别运行 Server 和 Client 代码。

### impala-beeswax

1. thrift 文件

* beeswax.thrift, ExecStats.thrift, ImpalaService.thrift, Status.thrift, Types.thrift ->
    来自于 https://github.com/cloudera/Impala/tree/cdh5-2.12.0_5.16.1/common/thrift

* hive_metastore.thrift, fb303.thrift ->
    来自于 https://github.com/cloudera/hue/tree/cdh5-3.9.0_5.16.1/apps/impala/thrift/include
    当然也可以从 hive 的源码 https://github.com/cloudera/hive/tree/cdh5-1.1.0_5.16.1/metastore/if 获取

* TCLIService.thrift ->
    来自于 https://github.com/cloudera/hue/tree/cdh5-3.9.0_5.16.1/apps/impala/thrift
    当然也可以从 hive 的源码 https://github.com/cloudera/hive/tree/cdh5-1.1.0_5.16.1/service/if 获取

* ErrorCodes.thrift ->
    来自于 https://github.com/cloudera/hue/tree/cdh5-3.9.0_5.16.1/apps/impala/thrift

2. 运行

* 运行 maven 插件，生成 java 代码。
* 直接运行 main 方法。

3. 参考

* [通过Impala thrift API接口进行Impala查询](https://blog.csdn.net/maydaysar/article/details/85236197)
* [impala-client-example](https://github.com/terry-chelsea/impala-client-example)