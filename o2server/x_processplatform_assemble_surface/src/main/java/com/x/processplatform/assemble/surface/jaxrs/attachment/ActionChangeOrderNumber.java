package com.x.processplatform.assemble.surface.jaxrs.attachment;

import java.util.List;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.annotation.CheckPersistType;
import com.x.base.core.project.exception.ExceptionAccessDenied;
import com.x.base.core.project.exception.ExceptionEntityNotExist;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WoId;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.processplatform.assemble.surface.Business;
import com.x.processplatform.assemble.surface.WorkControl;
import com.x.processplatform.core.entity.content.Attachment;

import io.swagger.v3.oas.annotations.media.Schema;

class ActionChangeOrderNumber extends BaseAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActionChangeOrderNumber.class);

	ActionResult<Wo> execute(EffectivePerson effectivePerson, String id, String workId, Integer order)
			throws Exception {

		LOGGER.debug("execute:{}, id:{}, workId:{}, order:{}.", effectivePerson::getDistinguishedName, () -> id,
				() -> workId, () -> order);

		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			ActionResult<Wo> result = new ActionResult<>();
			Business business = new Business(emc);
			Attachment attachment = emc.find(id, Attachment.class);
			if (null == attachment) {
				throw new ExceptionEntityNotExist(id, Attachment.class);
			}
			if (!business.readableWithWorkOrWorkCompleted(effectivePerson, workId)) {
				throw new ExceptionAccessDenied(effectivePerson);
			}
			List<String> identities = business.organization().identity().listWithPerson(effectivePerson);
			List<String> units = business.organization().unit().listWithPerson(effectivePerson);
			if (!this.edit(attachment, effectivePerson, identities, units, business)) {
				throw new ExceptionAccessDenied(effectivePerson, attachment);
			}
			emc.beginTransaction(Attachment.class);
			attachment.setOrderNumber(order);
			emc.check(attachment, CheckPersistType.all);
			emc.commit();
			Wo wo = new Wo();
			wo.setId(attachment.getId());
			result.setData(wo);
			return result;
		}
	}

	@Schema(name = "com.x.processplatform.assemble.surface.jaxrs.attachment.ActionChangeOrderNumber$Wo")
	public static class Wo extends WoId {

		private static final long serialVersionUID = 7781650524928249364L;

	}

}
