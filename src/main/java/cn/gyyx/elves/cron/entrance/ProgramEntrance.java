package cn.gyyx.elves.cron.entrance;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import cn.gyyx.elves.cron.dao.CronDao;
import cn.gyyx.elves.cron.job.JobTaskManager;
import cn.gyyx.elves.cron.pojo.TaskCron;
import cn.gyyx.elves.util.ExceptionUtil;
import cn.gyyx.elves.util.SpringUtil;
import cn.gyyx.elves.util.mq.PropertyLoader;
import cn.gyyx.elves.util.zk.ZookeeperExcutor;


/**
 * @ClassName: ProgramEntrance
 * @Description: Cron模块 程序入口
 * @author East.F
 * @date 2016年11月7日 上午9:29:31
 */
public class ProgramEntrance {
	
	private static final Logger LOG=Logger.getLogger(ProgramEntrance.class);
	
	/**
	 * 加载所有配置文件的路径
	 */
	private static void loadAllConfigFilePath(String configPath){
		SpringUtil.SPRING_CONFIG_PATH="file:"+configPath+File.separator+"conf"+File.separator+"spring.xml";
		SpringUtil.RABBITMQ_CONFIG_PATH="file:"+configPath+File.separator+"conf"+File.separator+"rabbitmq.xml";
		SpringUtil.PROPERTIES_CONFIG_PATH=configPath+File.separator+"conf"+File.separator+"conf.properties";
		SpringUtil.LOG4J_CONFIG_PATH=configPath+File.separator+"conf"+File.separator+"log4j.properties";
		SpringUtil.MYBATIS_CONFIG_PATH="file:"+configPath+File.separator+"conf"+File.separator+"mybatis.xml";
	}
	
	/**
	 * 加载日志配置文件
	 */
	private static void loadLogConfig() throws Exception{
		InputStream in=new FileInputStream(SpringUtil.LOG4J_CONFIG_PATH);// 自定义配置
		PropertyConfigurator.configure(in);
	}
	
	/**
	 * 加载Spring配置文件
	 */
	private static void loadApplicationXml() throws Exception{
		SpringUtil.app = new FileSystemXmlApplicationContext(SpringUtil.SPRING_CONFIG_PATH,SpringUtil.RABBITMQ_CONFIG_PATH);
	}
	
	/**
	 * @Title: registerZooKeeper
	 * @Description: 注册zookeeper
	 * @throws Exception 设定文件
	 * @return void    返回类型
	 */
	private static void registerZooKeeper() throws Exception{
		ZookeeperExcutor zke=new ZookeeperExcutor(PropertyLoader.ZOOKEEPER_HOST,
				PropertyLoader.ZOOKEEPER_OUT_TIME, PropertyLoader.ZOOKEEPER_OUT_TIME);
		String nodeName=zke.createNode(PropertyLoader.ZOOKEEPER_ROOT+"/Cron/", "");
		if(null!=nodeName){
			zke.addListener(PropertyLoader.ZOOKEEPER_ROOT+"/Cron/", "");
		}
	}
	
	
	/**
	 * @Title: loadCronJob
	 * @Description: 启动加载 flag=1的计划任务
	 * @throws Exception 设定文件
	 * @return void    返回类型
	 */
	public static void loadCronJob() throws Exception{
		//启动时  加载数据库中的cron任务（集群部署的时候会存在重复加载的问题）
		CronDao cronDao =SpringUtil.getBean(CronDao.class);
		List<TaskCron> list =cronDao.queryAllCron(1);
		LOG.info("find need to restart job size:"+list.size());
		if(list!=null&&list.size()>0){
			JobTaskManager jobTaskManager = SpringUtil.getBean(JobTaskManager.class);
			for(TaskCron cron :list){
				jobTaskManager.addJob(cron);
			}
		}
	}
	
	public static void main(String[] args) {
		//args = new String[]{"E:\\Git\\elves-cron\\cron"};
		if(null!=args&&args.length>0){
			try {
				loadAllConfigFilePath(args[0]);
				LOG.info("loadAllConfigFilePath success!");
				
		    	loadLogConfig();
				LOG.info("loadLogConfig success!");

				loadApplicationXml();
				LOG.info("loadApplicationXml success!");
				
				registerZooKeeper();
				LOG.info("registerZooKeeper success!");
				
				loadCronJob();
				LOG.info("load cron job success!");
			} catch (Exception e) {
				LOG.error("start cron error:"+ExceptionUtil.getStackTraceAsString(e));
				System.exit(1);
			}
    	}
		
	}
}
