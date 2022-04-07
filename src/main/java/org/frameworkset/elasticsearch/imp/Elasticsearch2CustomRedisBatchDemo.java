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
import org.frameworkset.nosql.redis.RedisTool;
import org.frameworkset.tran.CommonRecord;
import org.frameworkset.tran.DataRefactor;
import org.frameworkset.tran.DataStream;
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.es.input.dummy.ES2DummyExportBuilder;
import org.frameworkset.tran.input.file.FileTaskContext;
import org.frameworkset.tran.ouput.custom.CustomOutPut;
import org.frameworkset.tran.schedule.CallInterceptor;
import org.frameworkset.tran.schedule.TaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.ClusterPipeline;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>Description: 从ftp服务器下载excel文件，采集excel文件中数据并交给自定义处理器批量写入redis，redis配置参考resources/redis.xml配置文件</p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2021/2/1 14:39
 * @author biaoping.yin
 * @version 1.0
 */
public class Elasticsearch2CustomRedisBatchDemo {
	private static Logger logger = LoggerFactory.getLogger(Elasticsearch2CustomRedisBatchDemo.class);
	public static void main(String[] args){


		ES2DummyExportBuilder importBuilder = new ES2DummyExportBuilder();
		importBuilder.setBatchSize(10)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		/**
		 * es相关配置
		 */
		importBuilder
				.setDsl2ndSqlFile("dsl2ndSqlFile.xml")
				.setDslName("scrollQuery")
				.setScrollLiveTime("10m")
//				.setSliceQuery(true)
//				.setSliceSize(5)
				.setQueryUrl("dbdemo/_search")
				.addParam("fullImport",false)
//				//添加dsl中需要用到的参数及参数值
//				.addParam("var1","v1")
//				.addParam("var2","v2")
//				.addParam("var3","v3")
				.setIncreamentEndOffset(5);

		//自己处理数据
		importBuilder.setCustomOutPut(new CustomOutPut() {
			@Override
			public void handleData(TaskContext taskContext, List<CommonRecord> datas) {

				//You can do any thing here for datas


				ClusterPipeline clusterPipeline = null;
//				ClusterPipeline clusterPipeline1 = null;//可以写多个redis集群
				//批量处理
				try {
					clusterPipeline = RedisTool.getInstance().getClusterPipelined();
//					clusterPipeline1 = RedisTool.getInstance("redis1").getClusterPipelined();//可以写多个redis集群

					for (CommonRecord record : datas) {
						Map<String, Object> data = record.getDatas();
						String cert_no = (String)data.get("cert_no");
//					logger.info(SimpleStringUtil.object2json(data));
						String valuedata = SimpleStringUtil.object2json(data);
						logger.debug("cert_no:{}",cert_no);
						clusterPipeline.hset("xingchenma1", cert_no, valuedata);
//						clusterPipeline1.hset("xingchenma2", cert_no, valuedata);
					}
					clusterPipeline.sync();

//					clusterPipeline1.sync();//可以写多个redis集群
				}
				finally {
					if(clusterPipeline != null){
						clusterPipeline.close();
					}

//					if(clusterPipeline1 != null){//可以写多个redis集群
//						clusterPipeline1.close();
//					}
				}
			}
		});
		//增量配置开始
		importBuilder.setFromFirst(true);//setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
		//setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
		importBuilder.setLastValueStorePath("escustom_import");//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
		//增量配置结束

		//映射和转换配置开始

		/**
		 * 重新设置es数据结构
		 */
		importBuilder.setDataRefactor(new DataRefactor() {
			public void refactor(Context context) throws Exception  {
				//可以根据条件定义是否丢弃当前记录
				//context.setDrop(true);return;

				context.addFieldValue("collecttime",new Date());



			}
		});
		//映射和转换配置结束

		importBuilder.addCallInterceptor(new CallInterceptor() {
			@Override
			public void preCall(TaskContext taskContext) {

			}

			@Override
			public void afterCall(TaskContext taskContext) {
				if(taskContext != null) {
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
