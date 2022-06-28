package org.frameworkset.elasticsearch.imp;
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

import org.frameworkset.tran.DataRefactor;
import org.frameworkset.tran.DataStream;
import org.frameworkset.tran.config.ImportBuilder;
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.plugin.es.output.ElasticsearchOutputConfig;
import org.frameworkset.tran.plugin.file.input.FileInputConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Description: 从日志文件采集日志数据并保存到</p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2021/2/1 14:39
 * @author biaoping.yin
 * @version 1.0
 */
public class VOPSTestdevLog2ES {
	private static Logger logger = LoggerFactory.getLogger(VOPSTestdevLog2ES.class);
	public static void main(String[] args){


		ImportBuilder importBuilder = new ImportBuilder();
		importBuilder.setBatchSize(500)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);

		FileInputConfig config = new FileInputConfig();
		//.*.txt.[0-9]+$
		//[17:21:32:388]
//		config.addConfig(new FileConfig("D:\\ecslog",//指定目录
//				"error-2021-03-27-1.log",//指定文件名称，可以是正则表达式
//				"^\\[[0-9]{2}:[0-9]{2}:[0-9]{2}:[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
//				.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
//				.setMaxBytes(1048576)//控制每条日志的最大长度，超过长度将被截取掉
//				//.setStartPointer(1000l)//设置采集的起始位置，日志内容偏移量
//				.addField("tag","error") //添加字段tag到记录中
//				.setExcludeLines(new String[]{"\\[DEBUG\\]"}));//不采集debug日志

		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
				"business-handler.log",//指定文件名称，可以是正则表达式
				"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
				.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
				.addField("tag","business-handler")//添加字段tag到记录中
				.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);

		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"gateway-handler.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","gateway-handler")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);

		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"metrics-warn-job.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","metrics-warn-job")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);

		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"metrics-webdetector-node.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","metrics-webdetector-node")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);

		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"smsdata-job.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","smsdata-job")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);

		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"eccloginlog-handler.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","eccloginlog-handler")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);

		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"metrics-common-job.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","metrics-common-job")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);

		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"metrics-historydata-job.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","metrics-historydata-job")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);

		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"metrics-webdetector-handler.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","metrics-webdetector-handler")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);

		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"metrics-web.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","metrics-web")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);
		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"webpage-handler.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","webpage-handler")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);
		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"metrics-report.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","metrics-report")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);
		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"metrics-webdetector-job.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","metrics-webdetector-job")//添加字段tag到记录中
						.setEnableInode(true)
//				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
		);
		config.addConfig(new FileConfig("/home/log/visualops",//指定目录
						"smsdata-handler.log",//指定文件名称，可以是正则表达式
						"^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.addField("tag","smsdata-handler")//添加字段tag到记录中
						.setEnableInode(true)
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
		 *     "message": "[18:04:40:161] [INFO] - org.frameworkset.tran.schedule.ScheduleService.externalTimeSchedule(ScheduleService.java:192) - Execute schedule job Take 3 ms"
		 *   }
		 * }
		 *
		 * true 开启 false 关闭
		 */
		config.setEnableMeta(true);
		importBuilder.setInputConfig(config);
		//指定elasticsearch数据源名称，在application.properties文件中配置，default为默认的es数据源名称
		ElasticsearchOutputConfig elasticsearchOutputConfig = new ElasticsearchOutputConfig();
		elasticsearchOutputConfig.setTargetElasticsearch("default");
		//指定索引名称，这里采用的是elasticsearch 7以上的版本进行测试，不需要指定type
		elasticsearchOutputConfig.setIndex("filelog");
		//指定索引类型，这里采用的是elasticsearch 7以上的版本进行测试，不需要指定type
		//elasticsearchOutputConfig.setIndexType("idxtype");
		importBuilder.setOutputConfig(elasticsearchOutputConfig);
		//增量配置开始
		importBuilder.setFromFirst(false);//setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
		//setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
		importBuilder.setLastValueStorePath("filelog_import");//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
		//增量配置结束

		//映射和转换配置开始


		/**
		 * 重新设置es数据结构
		 */
		importBuilder.setDataRefactor(new DataRefactor() {
			public void refactor(Context context) throws Exception  {
				String tag = context.getStringValue("tag");
				if(tag != null) {
					context.setIndex("vops-dev-"+tag+"-{dateformat=yyyy.MM.dd}");
				}
				else {
					context.setIndex("vops-dev-{dateformat=yyyy.MM.dd}");
				}
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
		importBuilder.setAsyn(false);//true 异步方式执行，不等待所有导入作业任务结束，方法快速返回；false（默认值） 同步方式执行，等待所有导入作业任务结束，所有作业结束后方法才返回
		importBuilder.setPrintTaskLog(true);

		/**
		 * 启动es数据导入文件并上传sftp/ftp作业
		 */
		DataStream dataStream = importBuilder.builder();
		dataStream.execute();//启动同步作业
		logger.info("job started.");
	}
}
