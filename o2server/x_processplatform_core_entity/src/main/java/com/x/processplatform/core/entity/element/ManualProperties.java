package com.x.processplatform.core.entity.element;

import com.google.gson.JsonElement;
import com.x.base.core.entity.JsonProperties;
import com.x.base.core.project.annotation.FieldDescribe;

public class ManualProperties extends JsonProperties {

	private static final long serialVersionUID = -8141148907781411801L;
	@FieldDescribe("活动自定义数据")
	private JsonElement customData;

	@FieldDescribe("是否允许加签")
	private Boolean allowAddTask;

	public JsonElement getCustomData() {
		return customData;
	}

	public void setCustomData(JsonElement customData) {
		this.customData = customData;
	}

	public Boolean getAllowAddTask() {
		return allowAddTask;
	}

	public void setAllowAddTask(Boolean allowAddTask) {
		this.allowAddTask = allowAddTask;
	}

}
