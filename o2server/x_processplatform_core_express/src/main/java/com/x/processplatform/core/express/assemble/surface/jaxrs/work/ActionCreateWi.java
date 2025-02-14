package com.x.processplatform.core.express.assemble.surface.jaxrs.work;

import com.google.gson.JsonElement;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.gson.GsonPropertyObject;

import io.swagger.v3.oas.annotations.media.Schema;

public class ActionCreateWi extends GsonPropertyObject {

	private static final long serialVersionUID = 5156711320925350432L;

	@FieldDescribe("直接打开指定人员已经有的草稿,草稿判断:工作没有已办,只有一条此人的待办.")
	@Schema(description = "直接打开指定人员已经有的草稿,草稿判断:工作没有已办,只有一条此人的待办.")
	private Boolean latest;

	@FieldDescribe("标题.")
	@Schema(description = "标题.")
	private String title;

	@FieldDescribe("启动人员身份.")
	@Schema(description = "启动人员身份.")
	private String identity;

	@FieldDescribe("工作数据.")
	@Schema(description = "工作数据.")
	private JsonElement data;

	@FieldDescribe("父工作标识.")
	@Schema(description = "父工作标识.")
	private String parentWork;

	@FieldDescribe("允许启动非当但版本流程,默认否并自动升级到当前版本流程.")
	@Schema(description = "允许启动非当但版本流程,默认否并自动升级到当前版本流程.")
	private Boolean allowEdition;

	public String getParentWork() {
		return parentWork;
	}

	public void setParentWork(String parentWork) {
		this.parentWork = parentWork;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public JsonElement getData() {
		return data;
	}

	public void setData(JsonElement data) {
		this.data = data;
	}

	public Boolean getLatest() {
		return latest;
	}

	public void setLatest(Boolean latest) {
		this.latest = latest;
	}

	public Boolean getAllowEdition() {
		return allowEdition;
	}

	public void setAllowEdition(Boolean allowEdition) {
		this.allowEdition = allowEdition;
	}

}