package org.frameworkset.datatran.imp;
/**
 * Copyright 2020 bboss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.tran.DataRefactor;
import org.frameworkset.tran.DataStream;
import org.frameworkset.tran.config.ImportBuilder;
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.input.file.FileFilter;
import org.frameworkset.tran.input.file.FilterFileInfo;
import org.frameworkset.tran.plugin.db.output.DBOutputConfig;
import org.frameworkset.tran.plugin.file.input.FileInputConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * <p>Description: 从日志文件采集日志数据并保存到</p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2021/2/1 14:39
 * @author biaoping.yin
 * @version 1.0
 */
public class FileLog2DBDemo {
	private static Logger logger = LoggerFactory.getLogger(FileLog2DBDemo.class);
	public static void main(String[] args){

/**
 * 案例对应的表结构
 * CREATE TABLE
 *     filelog
 *     (
 *         id VARCHAR(100),
 *         MESSAGE text,
 *         title VARCHAR(1024),
 *         collecttime DATETIME,
 *         author VARCHAR(100),
 *         subtitle VARCHAR(450),
 *         optime DATETIME,
 *         path VARCHAR(200),
 *         hostname VARCHAR(100),
 *         pointer bigint(10),
 *         hostip VARCHAR(100),
 *         fileId VARCHAR(200),
 *         tag VARCHAR(45),
 *         PRIMARY KEY (id),
 *     )
 *     ENGINE=InnoDB DEFAULT CHARSET=utf8;
 */
		ImportBuilder importBuilder = new ImportBuilder();
		importBuilder.setBatchSize(500)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);
//":null,"jdbcFetchSize":-2147483648,"dbDriver":"com.mysql.cj.jdbc.Driver","dbUrl":"jdbc:mysql://192.168.137.1:3306/bboss?useUnicode=true&characterEncoding=utf-8&useSSL=false","dbUser":"root","dbPassword":"123456","initSize":100,"minIdleSize":100,"maxSize":100,"showSql":true,"usePool":true,"dbtype":null,"dbAdaptor":null,"columnLableUpperCase":false,"enableDBTransaction":false,"validateSQL":"select 1","dbName":"test"},"statusDbname":null,"statusTableDML":null,"fetchSize":10,"flushInterval":0,"ignoreNullValueField":false,"Elasticsearch":"default","sourceElasticsearch":"default","clientOptions":null,"geoipConfig":null,"sortLastValue":true,"useBatchContextIndexName":false,"discardBulkResponse":true,"debugResponse":false,"scheduleConfig":{"scheduleDate":null,"deyLay":1000,"period":10000,"fixedRate":false,"externalTimer":false},"importIncreamentConfig":{"lastValueColumn":"logOpertime","lastValue":null,"lastValueType":1,"lastValueStorePath":"es2dbdemo_import","lastValueStoreTableName":null,"lastValueDateType":true,"fromFirst":true,"statusTableId":null},"externalTimer":false,"printTaskLog":true,"applicationPropertiesFile":null,"configs":null,"batchSize":2,"parallel":true,"threadCount":50,"queue":10,"asyn":false,"continueOnError":true,"asynResultPollTimeOut":1000,"useLowcase":null,"scheduleBatchSize":null,"index":null,"indexType":null,"useJavaName":null,"exportResultHandlerClass":null,"locale":null,"timeZone":null,"esIdGeneratorClass":"org.frameworkset.tran.DefaultEsIdGenerator","dataRefactorClass":"org.frameworkset.datatran.imp.ES2DBScrollTimestampDemo$3","pagine":false,"scrollLiveTime":"10m","queryUrl":"dbdemo/_search","dsl2ndSqlFile":"dsl2ndSqlFile.xml","dslName":"scrollQuery","sliceQuery":false,"sliceSize":0,"Index":null,"IndexType":null}
		FileInputConfig config = new FileInputConfig();
        config.setCharsetEncode("GB2312");
		config.addConfig(new FileConfig().setSourcePath("D:\\logs")//指定目录
						.setFileHeadLineRegular("^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}:[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setFileFilter(new FileFilter() {
							@Override
							public boolean accept(FilterFileInfo fileInfo, FileConfig fileConfig) {
								//判断是否采集文件数据，返回true标识采集，false 不采集
								return fileInfo.getFileName().equals("metrics-report.log");
							}
						})//指定文件过滤器
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","elasticsearch")//添加字段tag到记录中
						.setEnableInode(false)
				//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);

//		config.addConfig("E:\\ELK\\data\\data3",".*.txt","^[0-9]{4}-[0-9]{2}-[0-9]{2}");
		/**
		 * 启用元数据信息到记录中，元数据信息以map结构方式作为@filemeta字段值添加到记录中，文件插件支持的元信息字段如下：
		 * hostIp：主机ip
		 * hostName：主机名称
		 * filePath： 文件路径
		 * timestamp：采集的时间戳
		 * pointer：记录对应的截止文件指针,long类型
		 * fileId：linux文件号，windows系统对应文件路径
		 * 例如：
		 * {
		 *   "_index": "filelog",
		 *   "_type": "_doc",
		 *   "_id": "HKErgXgBivowv_nD0Jhn",
		 *   "_version": 1,
		 *   "_score": null,
		 *   "_source": {
		 *     "title": "解放",
		 *     "subtitle": "小康",
		 *     "ipinfo": "",
		 *     "newcollecttime": "2021-03-30T03:27:04.546Z",
		 *     "author": "张无忌",
		 *     "@filemeta": {
		 *       "path": "D:\\ecslog\\error-2021-03-27-1.log",
		 *       "hostname": "",
		 *       "pointer": 3342583,
		 *       "hostip": "",
		 *       "timestamp": 1617074824542,
		 *       "fileId": "D:/ecslog/error-2021-03-27-1.log"
		 *     },
		 *     "@message": "[18:04:40:161] [INFO] - org.frameworkset.tran.schedule.ScheduleService.externalTimeSchedule(ScheduleService.java:192) - Execute schedule job Take 3 ms"
		 *   }
		 * }
		 *
		 * true 开启 false 关闭
		 */
		config.setEnableMeta(true);
		importBuilder.setInputConfig(config);
		//指定elasticsearch数据源名称，在application.properties文件中配置，default为默认的es数据源名称

//导出到数据源配置
		DBOutputConfig dbOutputConfig = new DBOutputConfig();
		dbOutputConfig
				.setSqlFilepath("sql-dbtran.xml")

				.setDbName("test")//指定目标数据库，在application.properties文件中配置
//				.setDbDriver("com.mysql.cj.jdbc.Driver") //数据库驱动程序，必须导入相关数据库的驱动jar包
//				.setDbUrl("jdbc:mysql://localhost:3306/bboss?useUnicode=true&allowPublicKeyRetrieval=true&characterEncoding=utf-8&useSSL=false&rewriteBatchedStatements=true")
//				.setDbUser("root")
//				.setDbPassword("123456")
//				.setValidateSQL("select 1")
//				.setUsePool(true)//是否使用连接池
				.setInsertSqlName("insertSql")//指定新增的sql语句名称，在配置文件中配置：sql-dbtran.xml

				/**
				 * 是否在批处理时，将insert、update、delete记录分组排序
				 * true：分组排序，先执行insert、在执行update、最后执行delete操作
				 * false：按照原始顺序执行db操作，默认值false
				 * @param optimize
				 * @return
				 */
				.setOptimize(false);//指定查询源库的sql语句，在配置文件中配置：sql-dbtran.xml
		importBuilder.setOutputConfig(dbOutputConfig);
		//增量配置开始
		importBuilder.setFromFirst(false);//setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
		//setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
		importBuilder.setLastValueStorePath("filelogdb_import");//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
		//增量配置结束

		//映射和转换配置开始
//		/**
//		 * db-es mapping 表字段名称到es 文档字段的映射：比如document_id -> docId
//		 * 可以配置mapping，也可以不配置，默认基于java 驼峰规则进行db field-es field的映射和转换
//		 */

		importBuilder.addFieldMapping("@message","message");

		importBuilder.addFieldMapping("@timestamp","optime");

		/**
		 * 重新设置es数据结构
		 */
		importBuilder.setDataRefactor(new DataRefactor() {
			public void refactor(Context context) throws Exception  {
				//可以根据条件定义是否丢弃当前记录
				//context.setDrop(true);return;
//				if(s.incrementAndGet() % 2 == 0) {
//					context.setDrop(true);
//					return;
//				}
//				System.out.println(data);
				if(context.getValue("id") == null){
					context.addFieldValue("id", SimpleStringUtil.getUUID());
				}

//				context.addFieldValue("author","duoduo");//将会覆盖全局设置的author变量
				context.addFieldValue("author","duoduo");
				context.addFieldValue("title","解放");
				context.addFieldValue("subtitle","小康");

				context.addFieldValue("collecttime",new Date());

				
//				//如果日志是普通的文本日志，非json格式，则可以自己根据规则对包含日志记录内容的message字段进行解析
//				String message = context.getStringValue("@message");
//				String[] fvs = message.split(" ");//空格解析字段
				/**
				 * //解析示意代码
				 * String[] fvs = message.split(" ");//空格解析字段
				 * //将解析后的信息添加到记录中
				 * context.addFieldValue("f1",fvs[0]);
				 * context.addFieldValue("f2",fvs[1]);
				 * context.addFieldValue("logVisitorial",fvs[2]);//包含ip信息
				 */
				//直接获取文件元信息
//				Map fileMata = (Map)context.getValue("@filemeta");
				/**
				 * 文件插件支持的元信息字段如下：
				 * hostIp：主机ip
				 * hostName：主机名称
				 * filePath： 文件路径
				 * timestamp：采集的时间戳
				 * pointer：记录对应的截止文件指针,long类型
				 * fileId：linux文件号，windows系统对应文件路径
				 */
				String filePath = (String)context.getMetaValue("filePath");
				String hostIp = (String)context.getMetaValue("hostIp");
				String hostName = (String)context.getMetaValue("hostName");
				String fileId = (String)context.getMetaValue("fileId");
				long pointer = (long)context.getMetaValue("pointer");
				context.addFieldValue("filePath",filePath);
				context.addFieldValue("hostIp",hostIp);
				context.addFieldValue("hostName",hostName);
				context.addFieldValue("fileId",fileId);
				context.addFieldValue("pointer",pointer);
				context.addIgnoreFieldMapping("@filemeta");

			}
		});
		//映射和转换配置结束

		/**
		 * 内置线程池配置，实现多线程并行数据导入功能，作业完成退出时自动关闭该线程池
		 */
		importBuilder.setParallel(true);//设置为多线程并行批量导入,false串行
		importBuilder.setQueue(10);//设置批量导入线程池等待队列长度
		importBuilder.setThreadCount(50);//设置批量导入线程池工作线程数量
		importBuilder.setContinueOnError(true);//任务出现异常，是否继续执行作业：true（默认值）继续执行 false 中断作业执行
		importBuilder.setPrintTaskLog(true);

		/**
		 * 启动es数据导入文件并上传sftp/ftp作业
		 */
		DataStream dataStream = importBuilder.builder();
		dataStream.execute();//启动同步作业
		logger.info("job started.");
	}
}
