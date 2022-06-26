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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.nosql.redis.RedisFactory;
import org.frameworkset.nosql.redis.RedisHelper;
import org.frameworkset.tran.CommonRecord;
import org.frameworkset.tran.DataRefactor;
import org.frameworkset.tran.DataStream;
import org.frameworkset.tran.config.ImportBuilder;
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.ftp.FtpConfig;
import org.frameworkset.tran.ftp.RemoteFileValidate;
import org.frameworkset.tran.ftp.ValidateContext;
import org.frameworkset.tran.input.excel.ExcelFileConfig;
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.input.file.FileFilter;
import org.frameworkset.tran.input.file.FileTaskContext;
import org.frameworkset.tran.input.file.FilterFileInfo;
import org.frameworkset.tran.ouput.custom.CustomOutPut;
import org.frameworkset.tran.plugin.custom.output.CustomOupputConfig;
import org.frameworkset.tran.plugin.file.input.ExcelFileInputConfig;
import org.frameworkset.tran.schedule.CallInterceptor;
import org.frameworkset.tran.schedule.TaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>Description:  从ftp服务器下载excel文件，采集excel文件中数据并交给自定义处理器按单条写入redis，redis配置参考resources/redis.xml配置文件</p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2021/2/1 14:39
 * @author biaoping.yin
 * @version 1.0
 */
public class FTPFileLog2CustomRedisDemo {
	private static Logger logger = LoggerFactory.getLogger(FTPFileLog2CustomRedisDemo.class);
	public static void main(String[] args){


		ImportBuilder importBuilder = new ImportBuilder();
		importBuilder.setBatchSize(10)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);
		ExcelFileInputConfig config = new ExcelFileInputConfig();

		FtpConfig ftpConfig = new FtpConfig().setFtpIP("10.13.6.127").setFtpPort(5322)
				.setFtpUser("ecs").setFtpPassword("ecs@123").setDownloadWorkThreads(4)
				.setRemoteFileDir("/home/ecs/failLog").setRemoteFileValidate(new RemoteFileValidate() {
					/**
					 * 校验数据文件合法性和完整性接口

					 * @param validateContext 封装校验数据文件信息
					 *     dataFile 待校验零时数据文件，可以根据文件名称获取对应文件的md5签名文件名、数据量稽核文件名称等信息，
					 *     remoteFile 通过数据文件对应的ftp/sftp文件路径，计算对应的目录获取md5签名文件、数据量稽核文件所在的目录地址
					 *     ftpContext ftp配置上下文对象
					 *     然后通过remoteFileAction下载md5签名文件、数据量稽核文件，再对数据文件进行校验即可
					 *     redownload 标记校验来源是否是因校验失败重新下载文件导致的校验操作，true 为重下后 文件校验，false为第一次下载校验
					 * @return int
					 * 文件内容校验成功
					 * 	RemoteFileValidate.FILE_VALIDATE_OK = 1;
					 * 	校验失败不处理文件
					 * 	RemoteFileValidate.FILE_VALIDATE_FAILED = 2;
					 * 	文件内容校验失败并备份已下载文件
					 * 	RemoteFileValidate.FILE_VALIDATE_FAILED_BACKUP = 3;
					 * 	文件内容校验失败并删除已下载文件
					 * 	RemoteFileValidate.FILE_VALIDATE_FAILED_DELETE = 5;
					 */
					public Result validateFile(ValidateContext validateContext)  {
						return Result.default_ok;
					}
				});
		//
		config.addConfig(new ExcelFileConfig()
				.addCellMapping(0,"shebao_org")
				.addCellMapping(1,"person_no")
				.addCellMapping(2,"name")
				.addCellMapping(3,"cert_type")

				.addCellMapping(4,"cert_no","")
				.addCellMapping(5,"zhs_item")

				.addCellMapping(6,"zhs_class")
				.addCellMapping(7,"zhs_sub_class")
				.addCellMapping(8,"zhs_year","2022")
				.addCellMapping(9,"zhs_level","1")
				.setFtpConfig(ftpConfig)
				.setFileFilter(new FileFilter() {//指定ftp文件筛选规则
					@Override
					public boolean accept(FilterFileInfo fileInfo, //Ftp文件名称
										  FileConfig fileConfig) {
						String name = fileInfo.getFileName();

						if(name.startsWith("湖南师大2021年新生医保（2021年）申报名单-合并"))
							return true;
						else
							return false;
					}
				})
				.setSkipHeaderLines(2)
				.setSourcePath("D:/ftplogs"))//指定目录
				;
		/**
		 * 备份采集完成文件
		 * true 备份
		 * false 不备份
		 */
		config.setBackupSuccessFiles(true);
		/**
		 * 备份文件目录
		 */
		config.setBackupSuccessFileDir("d:/ftpbackup");
		/**
		 * 备份文件清理线程执行时间间隔，单位：毫秒
		 * 默认每隔10秒执行一次
		 */
		config.setBackupSuccessFileInterval(20000l);
		/**
		 * 备份文件保留时长，单位：秒
		 * 默认保留7天
		 */
		config.setBackupSuccessFileLiveTime( 7 * 24 * 60 * 60l);
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

		//自己处理数据

		CustomOupputConfig customOupputConfig = new CustomOupputConfig();
		customOupputConfig.setCustomOutPut(new CustomOutPut() {
			@Override
			public void handleData(TaskContext taskContext, List<CommonRecord> datas) {

				//You can do any thing here for datas
				//单笔记录处理
				RedisHelper redisHelper = null;
				RedisHelper redisHelper1 = null;
				try {
					redisHelper = RedisFactory.getRedisHelper();
					redisHelper1 = RedisFactory.getRedisHelper("redis1");

					for (CommonRecord record : datas) {
						Map<String, Object> data = record.getDatas();
						String cert_no = (String)data.get("cert_no");
//					logger.info(SimpleStringUtil.object2json(data));
						String valuedata = SimpleStringUtil.object2json(data);
						redisHelper.hset("xingchenma", cert_no, valuedata);
						redisHelper.hset("xingchenma", cert_no, valuedata);
					}
				}
				finally {
					if(redisHelper != null)
						redisHelper.release();
					if(redisHelper1 != null)
						redisHelper1.release();
				}


			}
		});
		importBuilder.setOutputConfig(customOupputConfig);
		//增量配置开始
		importBuilder.setFromFirst(true);//setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
		//setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
		importBuilder.setLastValueStorePath("filelogcustom_import");//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
		//增量配置结束


		/**
		 * 重新设置es数据结构
		 */
		importBuilder.setDataRefactor(new DataRefactor() {
			public void refactor(Context context) throws Exception  {
				//可以根据条件定义是否丢弃当前记录
				//context.setDrop(true);return;

				context.addFieldValue("collecttime",new Date());

				

				//直接获取文件元信息
//				Map fileMata = (Map)context.getValue("@filemeta");
				/**
				 * 文件插件支持的元信息字段如下：
				 * ftpUser/ftpProtocol/ftpDir/ftpIp/ftpPort
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
				Date optime = (Date) context.getValue("@timestamp");
				long pointer = (long)context.getMetaValue("pointer");
				context.addFieldValue("optime",optime);
				context.addFieldValue("filePath",filePath);
				context.addFieldValue("hostIp",hostIp);
				context.addFieldValue("hostName",hostName);
				context.addFieldValue("fileId",fileId);
				context.addFieldValue("pointer",pointer);
				context.addIgnoreFieldMapping("@filemeta");

			}
		});
		//映射和转换配置结束

		importBuilder.addCallInterceptor(new CallInterceptor() {
			@Override
			public void preCall(TaskContext taskContext) {
				logger.info("preCall");
			}

			@Override
			public void afterCall(TaskContext taskContext) {
				if(taskContext != null) {
					taskContext.await();
					FileTaskContext fileTaskContext = (FileTaskContext)taskContext;
					logger.info("文件{}导入情况:{}",fileTaskContext.getFileInfo().getOriginFilePath(),taskContext.getJobTaskMetrics().toString());
				}
			}

			@Override
			public void throwException(TaskContext taskContext, Exception e) {
				if(taskContext != null) {
					taskContext.await();//等待数据异步处理完成
					FileTaskContext fileTaskContext = (FileTaskContext)taskContext;
					logger.info("文件{}导入情况:{}",fileTaskContext.getFileInfo().getOriginFilePath(),taskContext.getJobTaskMetrics().toString());
				}
			}
		});

		/**
		 * 内置线程池配置，实现多线程并行数据导入功能，作业完成退出时自动关闭该线程池
		 */
		importBuilder.setParallel(true);//设置为多线程并行批量导入,false串行
		importBuilder.setQueue(10);//设置批量导入线程池等待队列长度
		importBuilder.setThreadCount(5);//设置批量导入线程池工作线程数量
		importBuilder.setContinueOnError(true);//任务出现异常，是否继续执行作业：true（默认值）继续执行 false 中断作业执行
		importBuilder.setAsyn(false);//true 异步方式执行，不等待所有导入作业任务结束，方法快速返回；false（默认值） 同步方式执行，等待所有导入作业任务结束，所有作业结束后方法才返回
		importBuilder.setPrintTaskLog(false);

		/**
		 * 启动es数据导入文件并上传sftp/ftp作业
		 */
		DataStream dataStream = importBuilder.builder();
		dataStream.execute();//启动同步作业
		logger.info("job started.");
	}
}
