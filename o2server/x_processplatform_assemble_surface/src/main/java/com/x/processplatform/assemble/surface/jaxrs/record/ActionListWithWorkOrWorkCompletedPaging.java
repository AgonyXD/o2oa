package com.x.processplatform.assemble.surface.jaxrs.record;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.project.bean.WrapCopier;
import com.x.base.core.project.bean.WrapCopierFactory;
import com.x.base.core.project.config.Config;
import com.x.base.core.project.exception.ExceptionAccessDenied;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.base.core.project.tools.ListTools;
import com.x.processplatform.assemble.surface.Business;
import com.x.processplatform.assemble.surface.ThisApplication;
import com.x.processplatform.core.entity.content.Record;
import com.x.processplatform.core.entity.content.Task;
import com.x.processplatform.core.entity.content.WorkCompleted;

import io.swagger.v3.oas.annotations.media.Schema;

class ActionListWithWorkOrWorkCompletedPaging extends BaseAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActionListWithWorkOrWorkCompletedPaging.class);

	ActionResult<List<Wo>> execute(EffectivePerson effectivePerson, String workOrWorkCompleted, Integer page,
			Integer size) throws Exception {

		LOGGER.debug("execute:{}, workOrWorkCompleted:{}, page:{}, size:{}.", effectivePerson::getDistinguishedName,
				() -> workOrWorkCompleted, () -> page, () -> size);

		ActionResult<List<Wo>> result = new ActionResult<>();

		CompletableFuture<List<Wo>> listFuture = this.listFuture(workOrWorkCompleted, page, size);
		CompletableFuture<Boolean> checkControlFuture = this.checkControlFuture(effectivePerson, workOrWorkCompleted);

		if (BooleanUtils
				.isFalse(checkControlFuture.get(Config.processPlatform().getAsynchronousTimeout(), TimeUnit.SECONDS))) {
			throw new ExceptionAccessDenied(effectivePerson, workOrWorkCompleted);
		}

		result.setData(listFuture.get(Config.processPlatform().getAsynchronousTimeout(), TimeUnit.SECONDS));
		return result;

	}

	private CompletableFuture<List<Wo>> listFuture(String flag, Integer page, Integer size) {
		return CompletableFuture.supplyAsync(() -> {
			List<Wo> wos = new ArrayList<>();
			try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
				Business business = new Business(emc);
				String job = business.job().findWithWork(flag);
				if (null != job) {
					wos = emc.fetchEqualAscPaging(Record.class, Wo.copier, Record.job_FIELDNAME, job, page, size,
							Record.order_FIELDNAME);
				} else {
					job = business.job().findWithWorkCompleted(flag);
					WorkCompleted workCompleted = emc.firstEqual(WorkCompleted.class, WorkCompleted.job_FIELDNAME, job);
					if (ListTools.isNotEmpty(workCompleted.getProperties().getRecordList())) {
						List<Record> os = workCompleted.getProperties().getRecordList();
						int start = (page - 1) * size;
						start = Math.min(start, os.size());
						wos = Wo.copier.copy(os.stream().sorted(Comparator.comparing(Record::getOrder)).skip(start)
								.limit(size).collect(Collectors.toList()));
					} else {
						wos = emc.fetchEqualAscPaging(Record.class, Wo.copier, Record.job_FIELDNAME, job, page, size,
								Record.order_FIELDNAME);
					}
				}
				wos = emc.listEqual(Task.class, Task.job_FIELDNAME, job).stream()
						.sorted(Comparator.comparing(Task::getStartTime, Comparator.naturalOrder()))
						.map(o -> Wo.copier.copy(taskToRecord(o))).collect(Collectors.toList());
			} catch (Exception e) {
				LOGGER.error(e);
			}
			return wos;
		}, ThisApplication.threadPool());
	}

	private CompletableFuture<Boolean> checkControlFuture(EffectivePerson effectivePerson, String flag) {
		return CompletableFuture.supplyAsync(() -> {
			Boolean value = false;
			try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
				Business business = new Business(emc);
				value = business.readableWithWorkOrWorkCompleted(effectivePerson, flag);
			} catch (Exception e) {
				LOGGER.error(e);
			}
			return value;
		}, ThisApplication.threadPool());
	}

	private Record taskToRecord(Task task) {
		Record o = new Record();
		o.setType(Record.TYPE_CURRENTTASK);
		o.setFromActivity(task.getActivity());
		o.setFromActivityAlias(task.getActivityAlias());
		o.setFromActivityName(task.getActivityName());
		o.setFromActivityToken(task.getActivityToken());
		o.setFromActivityType(task.getActivityType());
		o.setPerson(task.getPerson());
		o.setIdentity(o.getIdentity());
		o.setUnit(task.getUnit());
		o.getProperties().setStartTime(task.getStartTime());
		o.getProperties().setEmpowerFromIdentity(task.getEmpowerFromIdentity());
		return o;
	}

	@Schema(name = "com.x.processplatform.assemble.surface.jaxrs.record.ActionListWithWorkOrWorkCompletedPaging$Wo")
	public static class Wo extends Record {

		private static final long serialVersionUID = -7666329770246726197L;

		static WrapCopier<Record, Wo> copier = WrapCopierFactory.wo(Record.class, Wo.class,
				JpaObject.singularAttributeField(Record.class, true, false), JpaObject.FieldsInvisible);

	}

}