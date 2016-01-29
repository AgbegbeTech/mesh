package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public class NodeRootImpl extends AbstractRootVertex<Node> implements NodeRoot {

	private static final Logger log = LoggerFactory.getLogger(NodeRootImpl.class);

	public static void checkIndices(Database database) {
		database.addEdgeIndex(HAS_NODE);
		database.addVertexType(NodeRootImpl.class);
	}

	@Override
	public Class<? extends Node> getPersistanceClass() {
		return NodeImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_NODE;
	}

	@Override
	public void addNode(Node node) {
		addItem(node);
	}

	@Override
	public void removeNode(Node node) {
		removeItem(node);
	}

	@Override
	public PageImpl<? extends Node> findAll(MeshAuthUser requestUser, List<String> languageTags, PagingParameter pagingInfo)
			throws InvalidArgumentException {
		VertexTraversal<?, ?, ?> traversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(NodeImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(NodeImpl.class);
		PageImpl<? extends Node> nodePage = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, NodeImpl.class);
		return nodePage;
	}

	@Override
	public Node create(User creator, SchemaContainer container, Project project) {
		// TODO check whether the mesh node is in fact a folder node.
		NodeImpl node = getGraph().addFramedVertex(NodeImpl.class);
		node.setSchemaContainer(container);

		// TODO is this a duplicate? - Maybe we should only store the project assignment in one way?
		project.getNodeRoot().addNode(node);
		node.setProject(project);
		node.setCreator(creator);
		node.setCreationTimestamp(System.currentTimeMillis());
		node.setEditor(creator);
		node.setLastEditedTimestamp(System.currentTimeMillis());

		addNode(node);
		return node;
	}

	@Override
	public void delete() {
		// TODO maybe add a check to prevent deletion of meshRoot.nodeRoot
		if (log.isDebugEnabled()) {
			log.debug("Deleting node root {" + getUuid() + "}");
		}
		for (Node node : findAll()) {
			node.delete();
		}
		getElement().remove();
	}

	private Observable<Node> createNode(InternalActionContext ac, Observable<SchemaContainer> obsSchemaContainer) {

		Database db = MeshSpringConfiguration.getInstance().database();
		Project project = ac.getProject();
		MeshAuthUser requestUser = ac.getUser();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		ServerSchemaStorage schemaStorage = ServerSchemaStorage.getSchemaStorage();

		return obsSchemaContainer.flatMap(schemaContainer -> {

			Observable<Tuple<SearchQueueBatch, Node>> obsTuple = db.noTrx(() -> {
				Schema schema = schemaContainer.getSchema();
				String body = ac.getBodyAsString();

				NodeCreateRequest requestModel = JsonUtil.readNode(body, NodeCreateRequest.class, schemaStorage);
				if (isEmpty(requestModel.getParentNodeUuid())) {
					throw error(BAD_REQUEST, "node_missing_parentnode_field");
				}
				if (isEmpty(requestModel.getLanguage())) {
					throw error(BAD_REQUEST, "node_no_languagecode_specified");
				}
				requestUser.reload();
				project.reload();
				// Load the parent node in order to create the node
				return project.getNodeRoot().loadObjectByUuid(ac, requestModel.getParentNodeUuid(), CREATE_PERM).map(parentNode -> {
					return db.trx(() -> {
						Node node = parentNode.create(requestUser, schemaContainer, project);
						requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
						node.setPublished(requestModel.isPublished());
						Language language = boot.languageRoot().findByLanguageTag(requestModel.getLanguage());
						if (language == null) {
							throw error(BAD_REQUEST, "language_not_found", requestModel.getLanguage());
						}
						NodeGraphFieldContainer container = node.getOrCreateGraphFieldContainer(language);
						container.updateFieldsFromRest(ac, requestModel.getFields(), schema);
						SearchQueueBatch batch = node.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
						return Tuple.tuple(batch, node);
					});
				});
			});
			return obsTuple.flatMap(tuple -> {
				return tuple.v1().process().map(i -> tuple.v2());
			});

		});
	}

	@Override
	public Observable<Node> create(InternalActionContext ac) {

		Database db = MeshSpringConfiguration.getInstance().database();

		return db.noTrx(() -> {

			Project project = ac.getProject();
			MeshAuthUser requestUser = ac.getUser();

			String body = ac.getBodyAsString();

			// 1. Extract the schema information from the given json
			SchemaReferenceInfo schemaInfo = JsonUtil.readValue(body, SchemaReferenceInfo.class);
			boolean missingSchemaInfo = schemaInfo.getSchema() == null
					|| (StringUtils.isEmpty(schemaInfo.getSchema().getUuid()) && StringUtils.isEmpty(schemaInfo.getSchema().getName()));
			if (missingSchemaInfo) {
				throw error(BAD_REQUEST, "error_schema_parameter_missing");
			}

			if (!isEmpty(schemaInfo.getSchema().getUuid())) {
				// 2. Use schema reference by uuid first
				return project.getSchemaContainerRoot().loadObjectByUuid(ac, schemaInfo.getSchema().getUuid(), READ_PERM).flatMap(schemaContainer -> {
					return createNode(ac, Observable.just(schemaContainer));
				});
			}

			// 3. Or just schema reference by name
			if (!isEmpty(schemaInfo.getSchema().getName())) {
				SchemaContainer containerByName = project.getSchemaContainerRoot().findByName(schemaInfo.getSchema().getName()).toBlocking().single();
				if (containerByName != null) {
					String schemaName = containerByName.getName();
					String schemaUuid = containerByName.getUuid();
					return requestUser.hasPermissionAsync(ac, containerByName, GraphPermission.READ_PERM).flatMap(hasPerm -> {
						if (hasPerm) {
							return createNode(ac, Observable.just(containerByName));
						} else {
							throw error(FORBIDDEN, "error_missing_perm", schemaUuid + "/" + schemaName);
						}
					});

				} else {
					throw error(NOT_FOUND, "schema_not_found", schemaInfo.getSchema().getName());
				}
			} else {
				throw error(BAD_REQUEST, "error_schema_parameter_missing");
			}

		});
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			for (Node node : findAll()) {
				// We don't need to recursively handle the permissions for each node again since this call will already affect all nodes.
				node.applyPermissions(role, false, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
	}

}
