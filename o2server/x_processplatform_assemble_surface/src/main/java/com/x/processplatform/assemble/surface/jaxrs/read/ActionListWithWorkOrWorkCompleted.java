package com.x.processplatform.assemble.surface.jaxrs.read;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.project.bean.WrapCopier;
import com.x.base.core.project.bean.WrapCopierFactory;
import com.x.base.core.project.exception.ExceptionAccessDenied;
import com.x.base.core.project.exception.ExceptionEntityNotExist;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.processplatform.assemble.surface.Business;
import com.x.processplatform.core.entity.content.Read;
import com.x.processplatform.core.entity.content.ReadCompleted;

class ActionListWithWorkOrWorkCompleted extends BaseAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActionListWithWorkOrWorkCompleted.class);

	ActionResult<List<Wo>> execute(EffectivePerson effectivePerson, String workOrWorkCompleted) throws Exception {
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			ActionResult<List<Wo>> result = new ActionResult<>();

			Business business = new Business(emc);

			if (!business.readableWithWorkOrWorkCompleted(effectivePerson, workOrWorkCompleted)) {
				throw new ExceptionAccessDenied(effectivePerson);
			}

			final String job = business.job().findWithWorkOrWorkCompleted(workOrWorkCompleted);

			List<Wo> wos = this.list(business, job);

			wos = wos.stream().sorted(Comparator.comparing(Wo::getCreateTime)).collect(Collectors.toList());

			result.setData(wos);
			return result;
		}
	}

	private List<Wo> list(Business business, String job) throws Exception {
		List<Read> os = business.entityManagerContainer().listEqual(Read.class, ReadCompleted.job_FIELDNAME, job);
		return Wo.copier.copy(os);
	}

	public static class Wo extends Read {

		static final long serialVersionUID = 5610132069178497370L;

		static WrapCopier<Read, Wo> copier = WrapCopierFactory.wo(Read.class, Wo.class, null,
				JpaObject.FieldsInvisible);

	}

}