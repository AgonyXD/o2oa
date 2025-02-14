package com.x.server.console.action;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.openjpa.persistence.OpenJPAPersistence;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.x.base.core.container.factory.PersistenceXmlHelper;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.entity.StorageObject;
import com.x.base.core.entity.annotation.ContainerEntity;
import com.x.base.core.entity.dataitem.DataItem;
import com.x.base.core.entity.dataitem.ItemCategory;
import com.x.base.core.entity.tools.JpaObjectTools;
import com.x.base.core.project.config.Config;
import com.x.base.core.project.config.DumpRestoreData;
import com.x.base.core.project.config.StorageMapping;
import com.x.base.core.project.config.StorageMappings;
import com.x.base.core.project.gson.XGsonBuilder;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.base.core.project.tools.DateTools;
import com.x.base.core.project.tools.ListTools;

import net.sf.ehcache.hibernate.management.impl.BeanUtils;

public class RestoreData {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestoreData.class);

	public boolean execute(String path) throws IOException, URISyntaxException {
		Date start = new Date();
		if (StringUtils.isEmpty(path)) {
			LOGGER.warn("{}.", () -> "path is empty.");
		}
		ClassLoader classLoader = EntityClassLoaderTools.concreteClassLoader();
		Path dir = dir(path);
		Thread thread = new Thread(new RunnableImpl(dir, start, classLoader));
		thread.start();
		return true;
	}

	private Path dir(String path) throws IOException, URISyntaxException {
		Path dir;
		if (BooleanUtils.isTrue(DateTools.isCompactDateTime(path))) {
			dir = Paths.get(Config.base(), "local", "dump", "dumpData_" + path);
		} else {
			dir = Paths.get(path);
			if ((!Files.exists(dir)) || (!Files.isDirectory(dir))) {
				throw new IllegalArgumentException("directory not exist: " + path + ".");
			} else if (dir.startsWith(Paths.get(Config.base()))) {
				throw new IllegalArgumentException("path can not in base directory.");
			}
		}
		return dir;
	}

	public class RunnableImpl implements Runnable {

		private Path dir;
		private Date start;
		private DumpRestoreDataCatalog catalog;
		private Gson gson;
		private ClassLoader classLoader;

		public RunnableImpl(Path dir, Date start, ClassLoader classLoader) throws IOException {
			this.dir = dir;
			this.start = start;
			this.classLoader = classLoader;
			Thread.currentThread().setContextClassLoader(classLoader);
			this.catalog = new DumpRestoreDataCatalog();
			this.gson = XGsonBuilder.instance();
			Path path = dir.resolve("catalog.json");
			this.catalog = XGsonBuilder.instance().fromJson(
					new String(Files.readAllBytes(path), StandardCharsets.UTF_8), DumpRestoreDataCatalog.class);
		}

		@Override
		public void run() {
			try {
				List<String> classNames = entities(catalog, classLoader);
				LOGGER.print("find: {} data to restore, path: {}.", classNames.size(), this.dir.toString());
				Path xml = Paths.get(Config.dir_local_temp_classes().getAbsolutePath(),
						DateTools.compact(start) + "_restore.xml");
				PersistenceXmlHelper.write(xml.toString(), classNames, true, classLoader);
				Stream<String> stream = BooleanUtils.isTrue(Config.dumpRestoreData().getParallel())
						? classNames.parallelStream()
						: classNames.stream();
				AtomicInteger idx = new AtomicInteger(1);
				AtomicLong total = new AtomicLong(0);
				stream.forEach(className -> {
					Thread.currentThread().setContextClassLoader(classLoader);
					try {
						@SuppressWarnings("unchecked")
						Class<JpaObject> cls = (Class<JpaObject>) Thread.currentThread().getContextClassLoader()
								.loadClass(className);
						LOGGER.print("restore data({}/{}): {}.", idx.getAndAdd(1), classNames.size(), cls.getName());
						long size = restore(cls, Config.dumpRestoreData().getAttachStorage(), xml);
						total.getAndAdd(size);
					} catch (Exception e) {
						LOGGER.error(new Exception(String.format("restore:%s error.", className), e));
					}
				});
				LOGGER.print("restore data completed, directory: {}, count: {}, total: {}, elapsed: {} minutes.",
						dir.toString(), idx.get(), total.longValue(),
						(System.currentTimeMillis() - start.getTime()) / 1000 / 60);
			} catch (Exception e) {
				LOGGER.error(e);
			}
		}

		private long restore(Class<?> cls, boolean attachStorage, Path xml) throws Exception {
			EntityManagerFactory emf = OpenJPAPersistence.createEntityManagerFactory(cls.getName(),
					xml.getFileName().toString(), PersistenceXmlHelper.properties(cls.getName(), false));
			EntityManager em = emf.createEntityManager();
			em.setFlushMode(FlushModeType.COMMIT);
			AtomicLong count = new AtomicLong(0);
			AtomicInteger batch = new AtomicInteger(1);
			try {
				Path directory = dir.resolve(cls.getName());
				if ((!Files.exists(directory)) || (!Files.isDirectory(directory))) {
					throw new ExceptionDirectoryNotExist(directory);
				}
				StorageMappings storageMappings = Config.storageMappings();
				if (!Objects.equals(Config.dumpRestoreData().getRestoreOverride(),
						DumpRestoreData.RESTOREOVERRIDE_SKIPEXISTED)) {
					this.clean(cls, em, storageMappings, cls.getAnnotation(ContainerEntity.class));
					em.clear();
				}
				List<Path> paths = this.list(directory);
				paths.stream().forEach(o -> {
					LOGGER.print("restore {}/{} part of data:{}.", batch.getAndAdd(1), paths.size(), cls.getName());
					try {
						restore(cls, o, em, attachStorage, storageMappings, count);
					} catch (Exception e) {
						LOGGER.error(new Exception(String.format("restore error with file:%s.", o.toString()), e));
					}
				});
				LOGGER.print("restore data: {} completed, count: {}.", cls.getName(), count.intValue());
			} catch (Exception e) {
				LOGGER.error(e);
			} finally {
				em.close();
				emf.close();
			}
			return count.longValue();
		}

		private void restore(Class<?> cls, Path o, EntityManager em, boolean attachStorage,
				StorageMappings storageMappings, AtomicLong count) throws Exception {
			em.getTransaction().begin();
			JsonArray raws = this.convert(o);
			for (JsonElement json : raws) {
				Object t = gson.fromJson(json, cls);
				if (Objects.equals(Config.dumpRestoreData().getRestoreOverride(),
						DumpRestoreData.RESTOREOVERRIDE_SKIPEXISTED)
						&& (null != em.find(cls, BeanUtils.getBeanProperty(t, JpaObject.id_FIELDNAME)))) {
					continue;
				}
				if (StorageObject.class.isAssignableFrom(cls) && attachStorage) {
					Path sub = o.resolveSibling(FilenameUtils.getBaseName(o.getFileName().toString()));
					this.binary(t, cls, sub, storageMappings);
				}
				em.persist(t);
				count.getAndAdd(1);
			}
			em.getTransaction().commit();
			em.clear();
		}

		private List<Path> list(Path directory) throws IOException {
			List<Path> list = new ArrayList<>();
			try (Stream<Path> stream = Files.list(directory)) {
				stream.filter(p -> StringUtils.endsWithIgnoreCase(p.getFileName().toString(), ".json"))
						.sorted((Path p1, Path p2) -> {
							Integer i1 = Integer.parseInt(FilenameUtils.getBaseName(p1.getFileName().toString()));
							Integer i2 = Integer.parseInt(FilenameUtils.getBaseName(p2.getFileName().toString()));
							return i1.compareTo(i2);
						}).forEach(list::add);
			}
			return list;
		}

		private List<String> entities(DumpRestoreDataCatalog catalog, ClassLoader classLoader) throws Exception {
			List<String> containerEntityNames = new ArrayList<>(JpaObjectTools.scanContainerEntityNames(classLoader));
			if (StringUtils.equalsIgnoreCase(DumpRestoreData.MODE_LITE, Config.dumpRestoreData().getMode())) {
				containerEntityNames = containerEntityNames.stream().filter(o -> {
					try {
						ContainerEntity containerEntity = classLoader.loadClass(o).getAnnotation(ContainerEntity.class);
						return Objects.equals(containerEntity.reference(), ContainerEntity.Reference.strong);
					} catch (Exception e) {
						LOGGER.error(e);
					}
					return false;
				}).collect(Collectors.toList());
			}
			List<String> classNames = new ArrayList<>(catalog.keySet());
			classNames = ListTools.includesExcludesWildcard(classNames, Config.dumpRestoreData().getIncludes(),
					Config.dumpRestoreData().getExcludes());
			return ListUtils.intersection(containerEntityNames, classNames);
		}

		@SuppressWarnings("unchecked")
		private void binary(Object o, Class<?> cls, Path sub, StorageMappings storageMappings) throws Exception {
			StorageObject so = (StorageObject) o;
			StorageMapping mapping = null;
			if (BooleanUtils.isTrue(Config.dumpRestoreData().getRedistributeStorage())) {
				mapping = storageMappings.random((Class<StorageObject>) cls);
			} else {
				mapping = storageMappings.get((Class<StorageObject>) cls, so.getStorage());
			}
			if (null == mapping) {
				throw new ExceptionMappingNotExist();
			}
			Path path = sub.resolve(Paths.get(so.path()).getFileName());
			if (!Files.exists(path)) {
				LOGGER.warn("file not exist: {}.", path.toString());
			} else {
				try (InputStream input = Files.newInputStream(path)) {
					so.saveContent(mapping, input, so.getName());
				}
			}
		}

		private JsonArray convert(Path path) throws IOException {
			// 必须先转换成 jsonElement 不能直接转成泛型T,如果直接转会有类型不匹配比如Integer变成了Double
			String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
			JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
			return jsonElement.getAsJsonArray();
		}

		private <T> void clean(Class<T> cls, EntityManager em, StorageMappings storageMappings,
				ContainerEntity containerEntity) throws Exception {
			List<T> list = null;
			do {
				if (ListTools.isNotEmpty(list)) {
					em.getTransaction().begin();
					for (T t : list) {
						if (StorageObject.class.isAssignableFrom(cls)) {
							StorageObject so = (StorageObject) t;
							@SuppressWarnings("unchecked")
							StorageMapping mapping = storageMappings.get((Class<StorageObject>) cls, so.getStorage());
							if (null != mapping) {
								so.deleteContent(mapping);
							}
						}
						em.remove(t);
					}
					em.getTransaction().commit();
				}
				list = list(cls, em, containerEntity);
			} while (ListTools.isNotEmpty(list));
		}

		private <T> List<T> list(Class<T> cls, EntityManager em, ContainerEntity containerEntity) throws Exception {
			List<T> list;
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<T> cq = cb.createQuery(cls);
			Root<T> root = cq.from(cls);
			Predicate p = cb.conjunction();
			if (StringUtils.equals(cls.getName(), "com.x.query.core.entity.Item")
					&& (StringUtils.isNotBlank(Config.dumpRestoreData().getItemCategory()))) {
				p = cb.and(p, cb.equal(root.get(DataItem.itemCategory_FIELDNAME),
						ItemCategory.valueOf(Config.dumpRestoreData().getItemCategory())));
			}
			list = em.createQuery(cq.select(root).where(p)).setMaxResults(containerEntity.dumpSize()).getResultList();
			return list;
		}
	}
}