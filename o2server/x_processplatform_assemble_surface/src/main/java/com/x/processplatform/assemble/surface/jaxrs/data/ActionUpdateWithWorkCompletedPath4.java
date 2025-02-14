package com.x.processplatform.assemble.surface.jaxrs.data;

import com.google.gson.JsonElement;
import com.x.base.core.project.Applications;
import com.x.base.core.project.x_processplatform_service_processing;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WoId;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.processplatform.assemble.surface.ThisApplication;
import com.x.processplatform.core.entity.content.WorkCompleted;

import io.swagger.v3.oas.annotations.media.Schema;

class ActionUpdateWithWorkCompletedPath4 extends BaseUpdateWithWorkCompletedPath {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ActionUpdateWithWorkCompletedPath4.class);

	ActionResult<Wo> execute(EffectivePerson effectivePerson, String id, String path0, String path1, String path2,
			String path3, String path4, JsonElement jsonElement) throws Exception {
		
		LOGGER.debug("execute:{}, id:{}.", effectivePerson::getDistinguishedName, () -> id);
		
		ActionResult<Wo> result = new ActionResult<>();
		WorkCompleted workCompleted = this.getWorkCompleted(effectivePerson, id);
		Wo wo = ThisApplication.context().applications()
				.putQuery(
						x_processplatform_service_processing.class, Applications.joinQueryUri("data", "workcompleted",
								workCompleted.getId(), path0, path1, path2, path3, path4),
						jsonElement, workCompleted.getJob())
				.getData(Wo.class);
		result.setData(wo);
		return result;
	}

	@Schema(name = "com.x.processplatform.assemble.surface.jaxrs.data.ActionUpdateWithWorkCompletedPath4$Wo")
	public static class Wo extends WoId {

		private static final long serialVersionUID = -6693056703851712131L;

	}

 
}
