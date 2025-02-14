package com.x.processplatform.service.processing.jaxrs.review;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.annotation.CheckPersistType;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.exception.ExceptionEntityNotExist;
import com.x.base.core.project.executor.ProcessPlatformExecutorFactory;
import com.x.base.core.project.gson.GsonPropertyObject;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WoId;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.base.core.project.tools.ListTools;
import com.x.processplatform.core.entity.content.Review;
import com.x.processplatform.core.entity.content.WorkCompleted;
import com.x.processplatform.service.processing.Business;

class ActionCreateWithWorkCompleted extends BaseAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActionCreateWithWorkCompleted.class);

	protected ActionResult<List<Wo>> execute(EffectivePerson effectivePerson, JsonElement jsonElement)
			throws Exception {

		LOGGER.debug("execute:{}.", effectivePerson::getDistinguishedName);

		Wi wi = this.convertToWrapIn(jsonElement, Wi.class);

		String executorSeed = null;

		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			WorkCompleted workCompleted = emc.fetch(wi.getWorkCompleted(), WorkCompleted.class,
					ListTools.toList(WorkCompleted.job_FIELDNAME));
			if (null == workCompleted) {
				throw new ExceptionEntityNotExist(wi.getWorkCompleted(), WorkCompleted.class);
			}
			executorSeed = workCompleted.getJob();
		}

		CallableImpl impl = new CallableImpl(wi);

		return ProcessPlatformExecutorFactory.get(executorSeed).submit(impl).get(300, TimeUnit.SECONDS);
	}

	private class CallableImpl implements Callable<ActionResult<List<Wo>>> {

		private Wi wi;

		private CallableImpl(Wi wi) {
			this.wi = wi;
		}

		@Override
		public ActionResult<List<Wo>> call() throws Exception {

			ActionResult<List<Wo>> result = new ActionResult<>();
			List<Wo> wos = new ArrayList<>();

			try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {

				Business business = new Business(emc);

				WorkCompleted workCompleted = emc.find(wi.getWorkCompleted(), WorkCompleted.class);
				if (null == workCompleted) {
					throw new ExceptionEntityNotExist(wi.getWorkCompleted(), WorkCompleted.class);
				}
				List<String> people = business.organization().person().list(wi.getPersonList());
				if (ListTools.isEmpty(people)) {
					throw new ExceptionPersonEmpty();
				}

				if (ListTools.isNotEmpty(people)) {
					emc.beginTransaction(Review.class);
					for (String person : people) {
						Long count = emc.countEqualAndEqual(Review.class, Review.job_FIELDNAME, workCompleted.getJob(),
								Review.person_FIELDNAME, person);
						if (count < 1) {
							Review review = new Review(workCompleted, person);
							emc.persist(review, CheckPersistType.all);
							Wo wo = new Wo();
							wo.setId(review.getId());
							wos.add(wo);
						}
					}
					emc.commit();
				}
			}
			result.setData(wos);
			return result;
		}
	}

	public static class Wi extends GsonPropertyObject {

		private static final long serialVersionUID = 8103324298350000501L;

		@FieldDescribe("已完成工作标识")
		private String workCompleted;

		@FieldDescribe("人员列表")
		private List<String> personList = new ArrayList<>();

		public String getWorkCompleted() {
			return workCompleted;
		}

		public void setWorkCompleted(String workCompleted) {
			this.workCompleted = workCompleted;
		}

		public List<String> getPersonList() {
			return personList;
		}

		public void setPersonList(List<String> personList) {
			this.personList = personList;
		}

	}

	public static class Wo extends WoId {

		private static final long serialVersionUID = -1087312273225002327L;
	}

}
