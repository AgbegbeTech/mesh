package com.gentics.mesh.search.index.microschema;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * Handler for the elastic search microschema index.
 */
@Singleton
public class MicroschemaContainerIndexHandlerImpl extends AbstractIndexHandler<HibMicroschema> implements MicroschemaIndexHandler {

	@Inject
	MicroschemaTransformer transformer;

	@Inject
	MicroschemaMappingProvider mappingProvider;

	@Inject
	SyncMetersFactory syncMetersFactory;

	@Inject
	public MicroschemaContainerIndexHandlerImpl(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper,
		MeshOptions options, SyncMetersFactory syncMetricsFactory, BucketManager bucketManager) {
		super(searchProvider, db, boot, helper, options, syncMetricsFactory, bucketManager);
	}

	@Override
	public String getType() {
		return "microschema";
	}

	@Override
	public Class<? extends HibBucketableElement> getElementClass() {
		return Microschema.class;
	}

	@Override
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.microschemaDao().globalCount();
		});
	}

	@Override
	public MicroschemaTransformer getTransformer() {
		return transformer;
	}

	@Override
	public MicroschemaMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
		return diffAndSync(Microschema.composeIndexName(), null, indexPattern);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, Microschema.composeIndexName());
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return Collections.singleton(Microschema.composeIndexName());
	}

	@Override
	public Function<String, HibMicroschema> elementLoader() {
		return (uuid) -> boot.meshRoot().getMicroschemaContainerRoot().findByUuid(uuid);
	}

	@Override
	public Stream<? extends HibMicroschema> loadAllElements() {
		return Tx.get().microschemaDao().findAll().stream();
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		String indexName = Microschema.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "microschema");
		return Collections.singletonMap(indexName, info);
	}

}
