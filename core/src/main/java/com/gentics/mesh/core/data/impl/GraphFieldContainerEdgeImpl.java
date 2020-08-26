package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.madl.field.FieldType.LINK;
import static com.gentics.mesh.madl.field.FieldType.STRING;
import static com.gentics.mesh.madl.field.FieldType.STRING_SET;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import java.util.Iterator;
import java.util.List;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.AbstractBasicGraphFieldContainerImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madl.field.FieldMap;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.Traversal;
import com.syncleus.ferma.traversals.TraversalFunction;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Edge;

/**
 * @see GraphFieldContainerEdge
 */
@GraphElement
public class GraphFieldContainerEdgeImpl extends MeshEdgeImpl implements GraphFieldContainerEdge {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(edgeType(GraphFieldContainerEdgeImpl.class.getSimpleName()));
		type.createType(edgeType(HAS_FIELD_CONTAINER).withSuperClazz(GraphFieldContainerEdgeImpl.class));

		FieldMap fields = new FieldMap();
		fields.put("out", LINK);
		fields.put(BRANCH_UUID_KEY, STRING);
		fields.put(EDGE_TYPE_KEY, STRING);
		fields.put(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, STRING);
		index.addCustomEdgeIndex(HAS_FIELD_CONTAINER, "branch_type_lang", fields, false);

		index.createIndex(edgeIndex(HAS_FIELD_CONTAINER)
			.withPostfix("field")
			.withField("out", LINK)
			.withField(BRANCH_UUID_KEY, STRING)
			.withField(EDGE_TYPE_KEY, STRING));

		// Webroot index:
		fields = new FieldMap();
		fields.put(BRANCH_UUID_KEY, STRING);
		fields.put(EDGE_TYPE_KEY, STRING);
		fields.put(WEBROOT_PROPERTY_KEY, STRING);
		index.addCustomEdgeIndex(HAS_FIELD_CONTAINER, WEBROOT_INDEX_POSTFIX_NAME, fields, true);

		// Webroot url field index:
		fields = new FieldMap();
		fields.put(BRANCH_UUID_KEY, STRING);
		fields.put(EDGE_TYPE_KEY, STRING);
		fields.put(WEBROOT_URLFIELD_PROPERTY_KEY, STRING_SET);
		index.addCustomEdgeIndex(HAS_FIELD_CONTAINER, WEBROOT_URLFIELD_INDEX_POSTFIX_NAME, fields, true);

	}

	public void setSegmentInfo(HibNode parentNode, String segment) {
		setSegmentInfo(composeSegmentInfo(parentNode, segment));
	}

	/**
	 * Creates the key for the webroot index.
	 *
	 * @param db
	 * @param segmentInfo
	 *            Value of the segment field
	 * @param branchUuid
	 *            Uuid of the branch
	 * @param type
	 *            Type of the container
	 * @return The composed key
	 */
	public static Object composeWebrootIndexKey(Database db, String segmentInfo, String branchUuid, ContainerType type) {
		return db.index().createComposedIndexKey(branchUuid, type.getCode(), segmentInfo);
	}

	public static String composeSegmentInfo(Node parentNode, String segment) {
		return parentNode == null ? "" : parentNode.getUuid() + segment;
	}

	public static Object composeWebrootUrlFieldIndexKey(Database db, String path, String branchUuid, ContainerType type) {
		return db.index().createComposedIndexKey(branchUuid, type.getCode(), path);
	}

	/**
	 * Extend the given traversal to filter edges that have one of the given language tags set (if languageTags is not null and not empty)
	 * 
	 * @param traversal
	 * @param languageTags
	 * @return
	 */
	public static EdgeTraversal<?, ?, ? extends VertexTraversal<?, ?, ?>> filterLanguages(
		EdgeTraversal<?, ?, ? extends VertexTraversal<?, ?, ?>> traversal, List<String> languageTags) {
		if (languageTags != null && languageTags.size() > 0) {
			LanguageRestrictionFunction[] pipes = new LanguageRestrictionFunction[languageTags.size()];
			for (int i = 0; i < languageTags.size(); i++) {
				pipes[i] = new LanguageRestrictionFunction(languageTags.get(0));
			}
			return traversal.or(pipes);
		} else {
			return traversal;
		}
	}

	@Override
	public String getLanguageTag() {
		return property(LANGUAGE_TAG_KEY);
	}

	@Override
	public void setLanguageTag(String languageTag) {
		property(LANGUAGE_TAG_KEY, languageTag);
	}

	@Override
	public BasicFieldContainer getContainer() {
		return outV(AbstractBasicGraphFieldContainerImpl.class).nextOrNull();
	}

	@Override
	public NodeGraphFieldContainer getNodeContainer() {
		return inV(NodeGraphFieldContainerImpl.class).nextOrNull();
	}

	@Override
	public Node getNode() {
		return outV(NodeImpl.class).nextOrNull();
	}

	@Override
	public ContainerType getType() {
		return ContainerType.get(property(EDGE_TYPE_KEY));
	}

	@Override
	public void setType(ContainerType type) {
		if (type == null) {
			property(EDGE_TYPE_KEY, null);
		} else {
			property(EDGE_TYPE_KEY, type.getCode());
		}
	}

	@Override
	public String getBranchUuid() {
		return property(BRANCH_UUID_KEY);
	}

	@Override
	public void setBranchUuid(String uuid) {
		property(BRANCH_UUID_KEY, uuid);
	}

	/**
	 * Traversal function that restricts by given language tag
	 */
	protected static class LanguageRestrictionFunction implements TraversalFunction<EdgeFrame, Traversal<?, ?, ?, ?>> {
		protected String languageTag;

		public LanguageRestrictionFunction(String languageTag) {
			this.languageTag = languageTag;
		}

		@Override
		public Traversal<?, ?, ?, ?> compute(EdgeFrame argument) {
			return argument.traversal().has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, languageTag);
		}
	}

	/**
	 * Check whether the node has a content (NGFC) for the branch and given type (Draft/Published/Initial).
	 * 
	 * @param nodeId
	 *            Object id of the node
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	public static boolean matchesBranchAndType(Object nodeId, String branchUuid, ContainerType type) {
		FramedGraph graph = Tx.get().getGraph();
		MeshComponent mesh = graph.getAttribute(GraphAttribute.MESH_COMPONENT);
		Iterable<Edge> edges = graph.getEdges("e." + HAS_FIELD_CONTAINER.toLowerCase() + "_field",
			mesh.database().index().createComposedIndexKey(nodeId, branchUuid, type.getCode()));
		return edges.iterator().hasNext();
	}

	public static GraphFieldContainerEdge findEdge(Object nodeId, String branchUuid, String code, String lang) {
		FramedGraph graph = Tx.get().getGraph();
		MeshComponent mesh = graph.getAttribute(GraphAttribute.MESH_COMPONENT);
		Iterable<Edge> edges = graph.getEdges("e." + HAS_FIELD_CONTAINER.toLowerCase() + "_branch_type_lang",
			mesh.database().index().createComposedIndexKey(nodeId, branchUuid, code, lang));
		Iterator<? extends GraphFieldContainerEdge> frames = graph.frameExplicit(edges.iterator(), GraphFieldContainerEdgeImpl.class);
		if (frames.hasNext()) {
			return frames.next();
		} else {
			return null;
		}
	}

	public static TraversalResult<GraphFieldContainerEdgeImpl> findEdges(Object nodeId, String branchUuid, ContainerType type) {
		FramedGraph graph = Tx.get().getGraph();
		MeshComponent mesh = graph.getAttribute(GraphAttribute.MESH_COMPONENT);
		Iterable<Edge> edges = graph.getEdges("e." + HAS_FIELD_CONTAINER.toLowerCase() + "_field",
			mesh.database().index().createComposedIndexKey(nodeId, branchUuid, type.getCode()));
		Iterator<GraphFieldContainerEdgeImpl> frames = graph.frameExplicit(edges.iterator(), GraphFieldContainerEdgeImpl.class);
		return new TraversalResult<>(() -> frames);
	}

}
