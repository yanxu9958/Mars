package com.mars.ioc.load;

import com.mars.core.annotation.MarsBean;
import com.mars.core.annotation.Resource;
import com.mars.core.constant.MarsConstant;
import com.mars.core.constant.MarsSpace;
import com.mars.core.logger.MarsLogger;
import com.mars.core.model.MarsBeanModel;
import com.mars.core.util.StringUtil;
import com.mars.ioc.factory.BeanFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 加载easyBean
 * @author yuye
 *
 */
public class LoadEasyBean {
	
	private static MarsLogger log = MarsLogger.getLogger(LoadEasyBean.class);
	
	/**
	 * 获取全局存储空间 
	 */
	private static MarsSpace constants = MarsSpace.getEasySpace();

	/**
	 * 创建easyBean对象，并完成对象注入
	 */
	@SuppressWarnings({ "unchecked" })
	public static void loadBean() throws Exception{
		try {
			/* 获取所有的bean数据 */
			Object objs = constants.getAttr(MarsConstant.MARS_BEANS);
			List<Map<String,Object>> easyBeans = null;
			if(objs != null) {
				easyBeans = (List<Map<String,Object>>)objs;
			} else {
				return;
			}
			
			/* 创建bean对象，并保存起来 */
			Object objs2 = constants.getAttr(MarsConstant.MARS_BEAN_OBJECTS);
			Map<String, MarsBeanModel> easyBeanObjs = new HashMap<>();
			if(objs2 != null) {
				easyBeanObjs = (Map<String, MarsBeanModel>)objs2;
			} 
			for(Map<String,Object> map : easyBeans) {
				
				Class<?> cls = (Class<?>)map.get("className");
				MarsBean marsBean = (MarsBean)map.get("annotation");
				
				String beanName = marsBean.value();
				if(beanName == null || beanName.equals("")) {
					beanName = StringUtil.getFirstLowerCase(cls.getSimpleName());
				}
				if(easyBeanObjs.get(beanName) == null) {
					MarsBeanModel beanModel = new MarsBeanModel();
					beanModel.setName(beanName);
					beanModel.setCls(cls);
					beanModel.setObj(BeanFactory.createBean(cls));
					easyBeanObjs.put(beanName, beanModel);
				} else {
					throw new Exception("已经存在name为["+beanName+"]的bean了");
				}
			}
			/* 注入对象 */
			iocBean(easyBeanObjs);
		} catch (Exception e) {
			throw new Exception("加载并注入EasyBean的时候出现错误",e);
		} 
	}
	
	/**
	 * easyBean注入
	 * @param easyBeanObjs 对象
	 */
	private static void iocBean(Map<String, MarsBeanModel> easyBeanObjs) throws Exception{
		
		try {
			for(String key : easyBeanObjs.keySet()) {
				MarsBeanModel marsBeanModel = easyBeanObjs.get(key);
				Object obj = marsBeanModel.getObj();
				Class<?> cls = marsBeanModel.getCls();
				/* 获取对象属性，完成注入 */
				Field[] fields = cls.getDeclaredFields();
				for(Field f : fields){
					Resource resource = f.getAnnotation(Resource.class);
					if(resource!=null){
						f.setAccessible(true);
						
						String filedName = resource.value();
						if(filedName == null || filedName.equals("")) {
							filedName = f.getName();
						}
						
						MarsBeanModel beanModel = easyBeanObjs.get(filedName);
						if(beanModel!=null){
							f.set(obj, beanModel.getObj());
							log.info(cls.getName()+"的属性"+f.getName()+"注入成功");
						}else{
							throw new Exception("不存在name为"+filedName+"的easyBean");
						}
					}
				}
				/* 保险起见，重新插入数据 */
				marsBeanModel.setCls(cls);
				easyBeanObjs.put(key, marsBeanModel);
			}
			
			constants.setAttr(MarsConstant.MARS_BEAN_OBJECTS, easyBeanObjs);
		} catch (Exception e) {
			throw new Exception("加载并注入EasyBean的时候出现错误",e);
		} 
	}
}
