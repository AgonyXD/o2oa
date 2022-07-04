package com.x.processplatform.assemble.surface.jaxrs.attachment;

import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.project.config.Config;
import com.x.base.core.project.config.ProcessPlatform.WorkExtensionEvent;
import com.x.base.core.project.config.StorageMapping;
import com.x.base.core.project.connection.CipherConnectionAction;
import com.x.base.core.project.exception.ExceptionAccessDenied;
import com.x.base.core.project.exception.ExceptionEntityNotExist;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WoFile;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.processplatform.assemble.surface.Business;
import com.x.processplatform.assemble.surface.ThisApplication;
import com.x.processplatform.assemble.surface.WorkControl;
import com.x.processplatform.core.entity.content.Attachment;
import com.x.processplatform.core.entity.content.Work;

import io.swagger.v3.oas.annotations.media.Schema;

class ActionDownloadWithWorkStream extends BaseAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActionDownloadWithWorkStream.class);

	ActionResult<Wo> execute(EffectivePerson effectivePerson, String id, String workId, String fileName)
			throws Exception {

		LOGGER.debug("execute:{}, id:{}, workId:{}.", effectivePerson::getDistinguishedName, () -> id, () -> workId);

		ActionResult<Wo> result = new ActionResult<>();
		Work work = null;
		Attachment attachment = null;
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {

			Business business = new Business(emc);
			work = emc.find(workId, Work.class);
			// 判断work是否存在
			if (null == work) {
				throw new ExceptionEntityNotExist(workId, Work.class);
			}
			// 判断attachment是否存在
			attachment = emc.find(id, Attachment.class);
			if (null == attachment) {
				throw new ExceptionEntityNotExist(id, Attachment.class);
			}
			// 生成当前用户针对work的权限控制,并判断是否可以访问
			Control control = business.getControl(effectivePerson, work, Control.class);
			if (BooleanUtils.isNotTrue(control.getAllowVisit())) {
				throw new ExceptionAccessDenied(effectivePerson, work);
			}
		}
		StorageMapping mapping = ThisApplication.context().storageMappings().get(Attachment.class,
				attachment.getStorage());
		if (StringUtils.isBlank(fileName)) {
			fileName = attachment.getName();
		} else {
			String extension = FilenameUtils.getExtension(fileName);
			if (StringUtils.isEmpty(extension)) {
				fileName = fileName + "." + attachment.getExtension();
			}
		}

		byte[] bytes = null;
		Optional<WorkExtensionEvent> event = Config.processPlatform().getExtensionEvents()
				.getWorkAttachmentDownloadEvents().bind(work.getApplication(), work.getProcess(), work.getActivity());
		if (event.isPresent()) {
			bytes = this.extensionService(effectivePerson, attachment.getId(), event.get());
		} else {
			bytes = attachment.readContent(mapping);
		}

		Wo wo = new Wo(bytes, this.contentType(true, fileName), this.contentDisposition(true, fileName));
		result.setData(wo);
		return result;
	}

	private byte[] extensionService(EffectivePerson effectivePerson, String id, WorkExtensionEvent event)
			throws Exception {
		byte[] bytes = null;
		Req req = new Req();
		req.setPerson(effectivePerson.getDistinguishedName());
		req.setAttachment(id);
		if (StringUtils.isNotEmpty(event.getCustom())) {
			bytes = ThisApplication.context().applications().postQueryBinary(event.getCustom(), event.getUrl(), req);
		} else {
			bytes = CipherConnectionAction.postBinary(effectivePerson.getDebugger(), event.getUrl(), req);
		}
		return bytes;
	}

	public static class Req {

		private String person;
		private String attachment;

		public String getPerson() {
			return person;
		}

		public void setPerson(String person) {
			this.person = person;
		}

		public String getAttachment() {
			return attachment;
		}

		public void setAttachment(String attachment) {
			this.attachment = attachment;
		}

	}

	public static class Control extends WorkControl {

		private static final long serialVersionUID = -754320243488525906L;

	}

	@Schema(name = "com.x.processplatform.assemble.surface.jaxrs.attachment.ActionDownloadWithWorkStream$Wo")
	public static class Wo extends WoFile {

		private static final long serialVersionUID = 1843284869726488698L;

		public Wo(byte[] bytes, String contentType, String contentDisposition) {
			super(bytes, contentType, contentDisposition);
		}

	}

}
