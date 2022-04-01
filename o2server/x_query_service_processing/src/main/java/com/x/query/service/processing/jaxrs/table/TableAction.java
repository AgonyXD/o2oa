package com.x.query.service.processing.jaxrs.table;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.gson.JsonElement;
import com.x.base.core.project.annotation.JaxrsDescribe;
import com.x.base.core.project.annotation.JaxrsMethodDescribe;
import com.x.base.core.project.annotation.JaxrsParameterDescribe;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.http.HttpMediaType;
import com.x.base.core.project.jaxrs.ResponseFactory;
import com.x.base.core.project.jaxrs.StandardJaxrsAction;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;

@Path("table")
@JaxrsDescribe("表")
public class TableAction extends StandardJaxrsAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(TableAction.class);

	@JaxrsMethodDescribe(value = "更新指定表中的行.", action = ActionUpdate.class)
	@POST
	@Path("{flag}/update/{bundle}")
	@Produces(HttpMediaType.APPLICATION_JSON_UTF_8)
	@Consumes(MediaType.APPLICATION_JSON)
	public void listNext(@Suspended final AsyncResponse asyncResponse, @Context HttpServletRequest request,
			@JaxrsParameterDescribe("标识") @PathParam("flag") String flag,
			@JaxrsParameterDescribe("数据行标识") @PathParam("bundle") String bundle, JsonElement jsonElement) {
		ActionResult<ActionUpdate.Wo> result = new ActionResult<>();
		EffectivePerson effectivePerson = this.effectivePerson(request);
		try {
			result = new ActionUpdate().execute(effectivePerson, flag, bundle, jsonElement);
		} catch (Exception e) {
			LOGGER.error(e, effectivePerson, request, jsonElement);
			result.error(e);
		}
		asyncResponse.resume(ResponseFactory.getEntityTagActionResultResponse(request, result));
	}

	@JaxrsMethodDescribe(value = "在服务器重新编译dynamicEntity之后需要重新初始化EntityManagerContainerFactory.", action = ActionReloadDynamic.class)
	@GET
	@Path("reload/dynamic")
	@Produces(HttpMediaType.APPLICATION_JSON_UTF_8)
	@Consumes(MediaType.APPLICATION_JSON)
	public void reloadDynamic(@Suspended final AsyncResponse asyncResponse, @Context HttpServletRequest request) {
		ActionResult<ActionReloadDynamic.Wo> result = new ActionResult<>();
		EffectivePerson effectivePerson = this.effectivePerson(request);
		try {
			result = new ActionReloadDynamic().execute(effectivePerson);
		} catch (Exception e) {
			LOGGER.error(e, effectivePerson, request, null);
			result.error(e);
		}
		asyncResponse.resume(ResponseFactory.getEntityTagActionResultResponse(request, result));
	}

	@JaxrsMethodDescribe(value = "获取可使用的自建表.", action = ActionListTableStatusBuild.class)
	@GET
	@Path("list/status/build")
	@Produces(HttpMediaType.APPLICATION_JSON_UTF_8)
	@Consumes(MediaType.APPLICATION_JSON)
	public void listTableStatusBuild(@Suspended final AsyncResponse asyncResponse,
			@Context HttpServletRequest request) {
		ActionResult<List<ActionListTableStatusBuild.Wo>> result = new ActionResult<>();
		EffectivePerson effectivePerson = this.effectivePerson(request);
		try {
			result = new ActionListTableStatusBuild().execute(effectivePerson);
		} catch (Exception e) {
			LOGGER.error(e, effectivePerson, request, null);
			result.error(e);
		}
		asyncResponse.resume(ResponseFactory.getEntityTagActionResultResponse(request, result));
	}

}
