package com.x.message.assemble.communicate.jaxrs.org;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.gson.GsonPropertyObject;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WrapNumber;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.message.core.entity.Org;

class ActionUpdateConsumed extends BaseAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActionUpdateConsumed.class);

	ActionResult<Wo> execute(EffectivePerson effectivePerson, String type, JsonElement jsonElement) throws Exception {

		LOGGER.debug("execute:{}, type:{}.", effectivePerson::getDistinguishedName, () -> type);
		
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			ActionResult<Wo> result = new ActionResult<>();
			Wi wi = this.convertToWrapIn(jsonElement, Wi.class);
			List<Org> orgs = emc.list(Org.class, wi.getIdList());

			if (!orgs.isEmpty()) {
				emc.beginTransaction(Org.class);
				for (Org o : orgs) {
					o.setConsumed(true);
				}
				emc.commit();
			}

			Wo wo = new Wo();
			wo.setValue(orgs.size());
			result.setData(wo);
			return result;
		}
	}

	public static class Wi extends GsonPropertyObject {

		private static final long serialVersionUID = 7876440743723658022L;
		
		@FieldDescribe("标识")
		private List<String> idList = new ArrayList<>();

		public List<String> getIdList() {
			return idList;
		}

		public void setIdList(List<String> idList) {
			this.idList = idList;
		}

	}

	public static class Wo extends WrapNumber {

		private static final long serialVersionUID = 6474672395918874871L;

	}

}
