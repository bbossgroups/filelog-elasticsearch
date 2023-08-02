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

import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.elasticsearch.serial.SerialUtil;
import org.frameworkset.tran.DataRefactor;
import org.frameworkset.tran.DataStream;
import org.frameworkset.tran.ExportResultHandler;
import org.frameworkset.tran.config.ImportBuilder;
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.ftp.FtpConfig;
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.input.file.FileFilter;
import org.frameworkset.tran.input.file.FilterFileInfo;
import org.frameworkset.tran.plugin.es.output.ElasticsearchOutputConfig;
import org.frameworkset.tran.plugin.file.input.FileInputConfig;
import org.frameworkset.tran.task.TaskCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * <p>Description: 增量扫描ftp目录中日志文件，下载未采集过的日志文件，
 * 然后采集日志数据并保存到elasticsearch，采集完毕后，备份日志文件到指定的目录下面，
 * 定期清理备份目录下超过指定时间的备份日志文件
 * </p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2021/2/1 14:39
 * @author biaoping.yin
 * @version 1.0
 */
public class SFtpLog2ESClearComplete2ndCloseOlderTimeDemo {
	private static Logger logger = LoggerFactory.getLogger(SFtpLog2ESClearComplete2ndCloseOlderTimeDemo.class);
	public static void main(String[] args){

		try {
			//清除测试表,导入的时候回重建表，测试的时候加上为了看测试效果，实际线上环境不要删表
//			String repsonse = ElasticSearchHelper.getRestClientUtil().dropIndice("errorlog");
			String repsonse = ElasticSearchHelper.getRestClientUtil().dropIndice("ftp-log");
			logger.info(repsonse);
		} catch (Exception e) {
		}
		ImportBuilder importBuilder = new ImportBuilder();
		importBuilder.setBatchSize(5000)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);
//		importBuilder.setSplitFieldName("@message");
//		importBuilder.setSplitHandler(new SplitHandler() {
//			@Override
//			public List<KeyMap<String, Object>> splitField(TaskContext taskContext,
//														   Record record, Object splitValue) {
//				Map<String,Object > data = (Map<String, Object>) record.getData();
//				List<KeyMap<String, Object>> splitDatas = new ArrayList<>();
//				//模拟将数据切割为10条记录
//				for(int i = 0 ; i < 10; i ++){
//					KeyMap<String, Object> d = new KeyMap<String, Object>();
//					d.put("message",i+"-"+(String)data.get("@message"));
////					d.setKey(SimpleStringUtil.getUUID());//如果是往kafka推送数据，可以设置推送的key
//					splitDatas.add(d);
//				}
//				return splitDatas;
//			}
//		});
		importBuilder.addFieldMapping("@message","message");
		FileInputConfig config = new FileInputConfig();
		config.setScanNewFileInterval(1*60*1000l);//每隔半1分钟扫描ftp目录下是否有最新ftp文件信息，采集完成或已经下载过的文件不会再下载采集
        config.setMaxFilesThreshold(2);//允许同时采集2个文件


//		config.setCharsetEncode("GB2312");
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

//		config.addConfig(new FileConfig("D:\\workspace\\bbossesdemo\\filelog-elasticsearch\\",//指定目录
//				"es.log",//指定文件名称，可以是正则表达式
//				"^\\[[0-9]{2}:[0-9]{2}:[0-9]{2}:[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
//				.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
//				.addField("tag","elasticsearch")//添加字段tag到记录中
//				.setEnableInode(false)
////				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
//				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
//		);
//		config.addConfig(new FileConfig("D:\\workspace\\bbossesdemo\\filelog-elasticsearch\\",//指定目录
//						new FileFilter() {
//							@Override
//							public boolean accept(File dir, String name, FileConfig fileConfig) {
//								//判断是否采集文件数据，返回true标识采集，false 不采集
//								return name.equals("es.log");
//							}
//						},//指定文件过滤器
//						"^\\[[0-9]{2}:[0-9]{2}:[0-9]{2}:[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
//						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
//						.addField("tag","elasticsearch")//添加字段tag到记录中
//						.setEnableInode(false)
////				.setIncludeLines(new String[]{".*ERROR.*"})//采集包含ERROR的日志
//				//.setExcludeLines(new String[]{".*endpoint.*"}))//采集不包含endpoint的日志
//		);
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		Date _startDate = null;
		try {
			_startDate = format.parse("20201211");//下载和采集2020年12月11日以后的数据文件
		} catch (ParseException e) {
			logger.error("",e);
		}
		final Date startDate = _startDate;
		FtpConfig ftpConfig = new FtpConfig().setFtpIP("localhost").setFtpPort(22)
				.setFtpUser("ecs").setFtpPassword("xxxx!")
				.setRemoteFileDir("/home/ecs/failLog")
				.setTransferProtocol(FtpConfig.TRANSFER_PROTOCOL_SFTP) ;//采用sftp协议
        FileConfig fileConfig = new FileConfig();
		config.addConfig(fileConfig.setFtpConfig(ftpConfig)
										.setFileFilter(new FileFilter() {//指定ftp文件筛选规则
											@Override
											public boolean accept(FilterFileInfo fileInfo, //Ftp文件名称
																  FileConfig fileConfig) {
												String name = fileInfo.getFileName();
//												//判断是否采集文件数据，返回true标识采集，false 不采集
//												boolean nameMatch = name.startsWith("731_tmrt_user_login_day_");
//												if(nameMatch){
//													String day = name.substring("731_tmrt_user_login_day_".length());
//													SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
//													try {
//														Date fileDate = format.parse(day);
//														if(fileDate.after(startDate))//下载和采集2020年12月11日以后的数据文件
//															return true;
//													} catch (ParseException e) {
//														logger.error("",e);
//													}
//
//
//												}
//												return false;
                                                return true;
											}
										})
                                .setScanChild(true).setCloseOlderTime(10000L)//静默关闭采集文件，并标记为采集完成
//										.addSkipScanNewFileTimeRange("11:30-13:00")
										.setSourcePath("D:/ftpchannel")//指定目录
										.addField("tag","elasticsearch")//添加字段tag到记录中
						);
        config.setCleanCompleteFiles(true);//设置是否清理完成文件，true清理，false不清理
//        config.setFileLiveTime(1000000L);//如果需要保留完成文件一定的时间，就可以设置，如果不设置，则立即清理完成文件
		config.setEnableMeta(true);
//		config.setJsondata(true);
		importBuilder.setInputConfig(config);
		//指定elasticsearch数据源名称，在application.properties文件中配置，default为默认的es数据源名称
		ElasticsearchOutputConfig elasticsearchOutputConfig = new ElasticsearchOutputConfig();
		elasticsearchOutputConfig.setTargetElasticsearch("default");
		//指定索引名称，这里采用的是elasticsearch 7以上的版本进行测试，不需要指定type
		elasticsearchOutputConfig.setIndex("ftp-log");
		//指定索引类型，这里采用的是elasticsearch 7以上的版本进行测试，不需要指定type
		//elasticsearchOutputConfig.setIndexType("idxtype");
		importBuilder.setOutputConfig(elasticsearchOutputConfig);
		//增量配置开始
		importBuilder.setFromFirst(false);//setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
		//setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
		importBuilder.setLastValueStorePath("ftploges_import");//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
		//增量配置结束

		//映射和转换配置开始
//		/**
//		 * db-es mapping 表字段名称到es 文档字段的映射：比如document_id -> docId
//		 * 可以配置mapping，也可以不配置，默认基于java 驼峰规则进行db field-es field的映射和转换
//		 */
//		importBuilder.addFieldMapping("document_id","docId")
//				.addFieldMapping("docwtime","docwTime")
//				.addIgnoreFieldMapping("channel_id");//添加忽略字段
//
//
//		/**
//		 * 为每条记录添加额外的字段和值
//		 * 可以为基本数据类型，也可以是复杂的对象
//		 */
//		importBuilder.addFieldValue("testF1","f1value");
//		importBuilder.addFieldValue("testInt",0);
//		importBuilder.addFieldValue("testDate",new Date());
//		importBuilder.addFieldValue("testFormateDate","yyyy-MM-dd HH",new Date());
//		TestObject testObject = new TestObject();
//		testObject.setId("testid");
//		testObject.setName("jackson");
//		importBuilder.addFieldValue("testObject",testObject);
		importBuilder.addFieldValue("author","张无忌");
//		importBuilder.addFieldMapping("operModule","OPER_MODULE");
//		importBuilder.addFieldMapping("logContent","LOG_CONTENT");


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

//				context.addFieldValue("author","duoduo");//将会覆盖全局设置的author变量
				context.addFieldValue("title","解放");
				context.addFieldValue("subtitle","小康");
				
				//如果日志是普通的文本日志，非json格式，则可以自己根据规则对包含日志记录内容的message字段进行解析
				String message = context.getStringValue("@message");
				String[] fvs = message.split(" ");//空格解析字段
				/**
				 * //解析示意代码
				 * String[] fvs = message.split(" ");//空格解析字段
				 * //将解析后的信息添加到记录中
				 * context.addFieldValue("f1",fvs[0]);
				 * context.addFieldValue("f2",fvs[1]);
				 * context.addFieldValue("logVisitorial",fvs[2]);//包含ip信息
				 */
				//直接获取文件元信息
				Map fileMata = (Map)context.getValue("@filemeta");
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
				//可以根据文件路径信息设置不同的索引
//				if(filePath.endsWith("metrics-report.log")) {
//					context.setIndex("metrics-report");
//				}
//				else if(filePath.endsWith("es.log")){
//					 context.setIndex("eslog");
//				}


//				context.addIgnoreFieldMapping("title");
				//上述三个属性已经放置到docInfo中，如果无需再放置到索引文档中，可以忽略掉这些属性
//				context.addIgnoreFieldMapping("author");

//				//修改字段名称title为新名称newTitle，并且修改字段的值
//				context.newName2ndData("title","newTitle",(String)context.getValue("title")+" append new Value");
				/**
				 * 获取ip对应的运营商和区域信息
				 */
				/**
				IpInfo ipInfo = (IpInfo) context.getIpInfo(fvs[2]);
				if(ipInfo != null)
					context.addFieldValue("ipinfo", ipInfo);
				else{
					context.addFieldValue("ipinfo", "");
				}*/
				DateFormat dateFormat = SerialUtil.getDateFormateMeta().toDateFormat();
//				Date optime = context.getDateValue("LOG_OPERTIME",dateFormat);
//				context.addFieldValue("logOpertime",optime);
				context.addFieldValue("newcollecttime",new Date());

				/**
				 //关联查询数据,单值查询
				 Map headdata = SQLExecutor.queryObjectWithDBName(Map.class,context.getEsjdbc().getDbConfig().getDbName(),
				 "select * from head where billid = ? and othercondition= ?",
				 context.getIntegerValue("billid"),"otherconditionvalue");//多个条件用逗号分隔追加
				 //将headdata中的数据,调用addFieldValue方法将数据加入当前es文档，具体如何构建文档数据结构根据需求定
				 context.addFieldValue("headdata",headdata);
				 //关联查询数据,多值查询
				 List<Map> facedatas = SQLExecutor.queryListWithDBName(Map.class,context.getEsjdbc().getDbConfig().getDbName(),
				 "select * from facedata where billid = ?",
				 context.getIntegerValue("billid"));
				 //将facedatas中的数据,调用addFieldValue方法将数据加入当前es文档，具体如何构建文档数据结构根据需求定
				 context.addFieldValue("facedatas",facedatas);
				 */
			}
		});
		//映射和转换配置结束
		importBuilder.setExportResultHandler(new ExportResultHandler() {
			@Override
			public void success(TaskCommand taskCommand, Object o) {
//				logger.info("result:"+o);
			}

			@Override
			public void error(TaskCommand taskCommand, Object o) {
				logger.warn("error:"+o);
			}

			@Override
			public void exception(TaskCommand taskCommand, Throwable exception) {
				logger.warn("error:",exception);
			}


		});
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
