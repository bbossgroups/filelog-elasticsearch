package org.frameworkset.elasticsearch.imp.metrics;
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
import org.frameworkset.elasticsearch.bulk.*;
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
import org.frameworkset.tran.metrics.entity.KeyMetric;
import org.frameworkset.tran.metrics.entity.MapData;
import org.frameworkset.tran.metrics.job.KeyMetricBuilder;
import org.frameworkset.tran.metrics.job.Metrics;
import org.frameworkset.tran.metrics.job.builder.MetricBuilder;
import org.frameworkset.tran.plugin.custom.output.CustomOutPut;
import org.frameworkset.tran.plugin.custom.output.CustomOutputConfig;
import org.frameworkset.tran.plugin.file.input.ExcelFileInputConfig;
import org.frameworkset.tran.plugin.metrics.output.ETLMetrics;
import org.frameworkset.tran.schedule.CallInterceptor;
import org.frameworkset.tran.schedule.TaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <p>Description:  从ftp服务器下载excel文件，采集excel文件中数据，交给指标计算器进行统计计算
 * 单个metrics，metrics处理2类指标
 * </p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2021/2/1 14:39
 * @author biaoping.yin
 * @version 1.0
 */
public class FTPFileLog2Metrics2Metric2Custom {
	private static Logger logger = LoggerFactory.getLogger(FTPFileLog2Metrics2Metric2Custom.class);
	public static void main(String[] args){


		ImportBuilder importBuilder = new ImportBuilder();
		importBuilder.setBatchSize(10)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);
		ExcelFileInputConfig config = new ExcelFileInputConfig();

		FtpConfig ftpConfig = new FtpConfig().setFtpIP("10.13.6.127").setFtpPort(5322)
				.setFtpUser("ecs").setFtpPassword("ecs@123").setDownloadWorkThreads(4)
				.setRemoteFileDir("/home/ecs/excelfiles").setRemoteFileValidate(new RemoteFileValidate() {
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
		FileConfig excelFileConfig = new ExcelFileConfig();
		excelFileConfig
				.addCellMapping(0,"shebao_org")
				.addCellMapping(1,"person_no")
				.addCellMapping(2,"name")
				.addCellMapping(3,"cert_type")

				.addCellMapping(4,"cert_no","")
				.addCellMapping(5,"zhs_item")

				.addCellMapping(6,"zhs_class")
				.addCellMapping(7,"zhs_sub_class")
				.addCellMapping(8,"zhs_year","2022")
				.addCellMapping(9,"zhs_level","1");
		excelFileConfig.setFtpConfig(ftpConfig)
				.setFileFilter(new FileFilter() {//指定ftp文件筛选规则
					@Override
					public boolean accept(FilterFileInfo fileInfo, //Ftp文件名称
										  FileConfig fileConfig) {
						String name = fileInfo.getFileName();

						if(name.startsWith("师大2021年新生医保（2021年）申报名单-合并"))
							return true;
						else
							return false;
					}
				})
				.setSkipHeaderLines(2)
				.setSourcePath("D:/ftplogs");
		config.addConfig(excelFileConfig)//指定目录
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

		BulkProcessorBuilder bulkProcessorBuilder = new BulkProcessorBuilder();
		bulkProcessorBuilder.setBlockedWaitTimeout(-1)//指定bulk工作线程缓冲队列已满时后续添加的bulk处理排队等待时间，如果超过指定的时候bulk将被拒绝处理，单位：毫秒，默认为0，不拒绝并一直等待成功为止

				.setBulkSizes(200)//按批处理数据记录数
				.setFlushInterval(5000)//强制bulk操作时间，单位毫秒，如果自上次bulk操作flushInterval毫秒后，数据量没有满足BulkSizes对应的记录数，但是有记录，那么强制进行bulk处理

				.setWarnMultsRejects(1000)//由于没有空闲批量处理工作线程，导致bulk处理操作出于阻塞等待排队中，BulkProcessor会对阻塞等待排队次数进行计数统计，bulk处理操作被每被阻塞排队WarnMultsRejects次（1000次），在日志文件中输出拒绝告警信息
				.setWorkThreads(10)//bulk处理工作线程数
				.setWorkThreadQueue(50)//bulk处理工作线程池缓冲队列大小
				.setBulkProcessorName("detail_bulkprocessor")//工作线程名称，实际名称为BulkProcessorName-+线程编号
				.setBulkRejectMessage("detail bulkprocessor")//bulk处理操作被每被拒绝WarnMultsRejects次（1000次），在日志文件中输出拒绝告警信息提示前缀
				.setElasticsearch("default")//指定明细Elasticsearch集群数据源名称，bboss可以支持多数据源
				.setFilterPath(BulkConfig.ERROR_FILTER_PATH)
				.addBulkInterceptor(new BulkInterceptor() {
					public void beforeBulk(BulkCommand bulkCommand) {

					}

					public void afterBulk(BulkCommand bulkCommand, String result) {
						if(logger.isDebugEnabled()){
							logger.debug(result);
						}
					}

					public void exceptionBulk(BulkCommand bulkCommand, Throwable exception) {
						if(logger.isErrorEnabled()){
							logger.error("exceptionBulk",exception);
						}
					}
					public void errorBulk(BulkCommand bulkCommand, String result) {
						if(logger.isWarnEnabled()){
							logger.warn(result);
						}
					}
				})//添加批量处理执行拦截器，可以通过addBulkInterceptor方法添加多个拦截器
		;
		/**
		 * 构建BulkProcessor批处理组件，一般作为单实例使用，单实例多线程安全，可放心使用
		 */
		BulkProcessor bulkProcessor = bulkProcessorBuilder.build();//构建批处理作业组件
		ETLMetrics keyMetrics = new ETLMetrics(Metrics.MetricsType_KeyTimeMetircs){
			@Override
			public void builderMetrics(){
				//指标1 按证书类型统计
				addMetricBuilder(new MetricBuilder() {
					@Override
					public String buildMetricKey(MapData mapData){
						CommonRecord data = (CommonRecord) mapData.getData();
						String cert_type = (String) data.getData("cert_type");
						return cert_type;
					}
					@Override
					public KeyMetricBuilder metricBuilder(){
						return new KeyMetricBuilder() {
							@Override
							public KeyMetric build() {
								return new PersonMetric();
							}
						};
					}
				});

				//指标2 按照征收项目统计
				addMetricBuilder(new MetricBuilder() {
					@Override
					public String buildMetricKey(MapData mapData){
						CommonRecord data = (CommonRecord) mapData.getData();
						String zhs_item = (String) data.getData("zhs_item");
						return zhs_item;
					}
					@Override
					public KeyMetricBuilder metricBuilder(){
						return new KeyMetricBuilder() {
							@Override
							public KeyMetric build() {
								return new ZhsItemMetric();
							}
						};
					}
				});
				// key metrics中包含两个segment(S0,S1)
				setSegmentBoundSize(5000000);
				setTimeWindows(60 );
			}

			@Override
			public void persistent(Collection< KeyMetric> metrics) {
				metrics.forEach(keyMetric->{
					if(keyMetric instanceof PersonMetric) {
						PersonMetric testKeyMetric = (PersonMetric) keyMetric;
						Map esData = new HashMap();
						esData.put("dataTime", testKeyMetric.getDataTime());
						esData.put("hour", testKeyMetric.getDayHour());
						esData.put("minute", testKeyMetric.getMinute());
						esData.put("day", testKeyMetric.getDay());
						esData.put("metric", testKeyMetric.getMetric());
						esData.put("certType", testKeyMetric.getCertType());
						esData.put("count", testKeyMetric.getCount());
						bulkProcessor.insertData("vops-personmetricscustom", esData);
					}
					else if(keyMetric instanceof ZhsItemMetric) {
						ZhsItemMetric testKeyMetric = (ZhsItemMetric) keyMetric;
						Map esData = new HashMap();
						esData.put("dataTime", testKeyMetric.getDataTime());
						esData.put("hour", testKeyMetric.getDayHour());
						esData.put("minute", testKeyMetric.getMinute());
						esData.put("day", testKeyMetric.getDay());
						esData.put("metric", testKeyMetric.getMetric());
						esData.put("zhsItem", testKeyMetric.getZhsItem());
						esData.put("count", testKeyMetric.getCount());
						bulkProcessor.insertData("vops-zhsmetricscustom", esData);
					}

				});

			}
		};



		importBuilder.addMetrics(keyMetrics);

		//自己处理数据
		CustomOutputConfig customOutputConfig = new CustomOutputConfig();
		customOutputConfig.setCustomOutPut(new CustomOutPut() {
			@Override
			public void handleData(TaskContext taskContext, List<CommonRecord> datas) {

				//You can do any thing here for datas
				for(CommonRecord record:datas){
					Map<String,Object> data = record.getDatas();
					logger.info("-----------------"+SimpleStringUtil.object2json(data));
				}
			}
		});
		importBuilder.setOutputConfig(customOutputConfig);
		//增量配置开始
		importBuilder.setFromFirst(true);//setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
		//setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
		importBuilder.setLastValueStorePath("FTPFileLog2Metrics2Metric2Custom_import");//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
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
			public void throwException(TaskContext taskContext, Throwable e) {
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
		importBuilder.setPrintTaskLog(false);

		/**
		 * 启动es数据导入文件并上传sftp/ftp作业
		 */
		DataStream dataStream = importBuilder.builder();
		dataStream.execute();//启动同步作业
		logger.info("job started.");
	}
}
