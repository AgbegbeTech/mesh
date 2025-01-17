package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.search.BucketableElement;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.Errors;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.util.VersionNumber;

/**
 * A node field container is an aggregation node that holds localized fields (e.g.: StringField, NodeField...)
 */
public interface NodeGraphFieldContainer extends GraphFieldContainer, EditorTrackingVertex, BucketableElement {
	/**
	 * Delete the field container. This will also delete linked elements like lists. If the container has a "next" container, that container will be deleted as
	 * well.
	 * 
	 * @param bac
	 */
	void delete(BulkActionContext bac);

	/**
	 * Delete the field container. This will also delete linked elements like lists.
	 * 
	 * @param bac
	 * @param deleteNext
	 *            true to also delete all "next" containers, false to only delete this container
	 */
	void delete(BulkActionContext bac, boolean deleteNext);

	/**
	 * "Delete" the field container from the branch. This will not actually delete the container itself, but will remove DRAFT and PUBLISHED edges
	 *
	 * @param branch
	 * @param bac
	 */
	void deleteFromBranch(HibBranch branch, BulkActionContext bac);

	/**
	 * Return the display field value for this container.
	 * 
	 * @return
	 */
	String getDisplayFieldValue();

	/**
	 * Get the node to which this container belongs.
	 *
	 * @return
	 */
	Node getNode();

	/**
	 * Update the property webroot path info. This will also check for uniqueness conflicts of the webroot path and will throw a
	 * {@link Errors#conflict(String, String, String, String...)} if one found.
	 * 
	 * @param ac
	 * @param branchUuid
	 *            branch Uuid
	 * @param conflictI18n
	 *            key of the message in case of conflicts
	 */
	void updateWebrootPathInfo(InternalActionContext ac, String branchUuid, String conflictI18n);

	/**
	 * Update the property webroot path info. This will also check for uniqueness conflicts of the webroot path and will throw a
	 * {@link Errors#conflict(String, String, String, String...)} if one found.
	 * 
	 * @param branchUuid
	 * @param conflictI18n
	 */
	default void updateWebrootPathInfo(String branchUuid, String conflictI18n) {
		updateWebrootPathInfo(null, branchUuid, conflictI18n);
	}

	/**
	 * Get the Version Number or null if no version set.
	 * 
	 * @return Version Number
	 */
	VersionNumber getVersion();

	/**
	 * Set the Version Number.
	 * 
	 * @param version
	 */
	void setVersion(VersionNumber version);

	/**
	 * Check whether the field container has a next version
	 * 
	 * @return true if the field container has a next version
	 */
	boolean hasNextVersion();

	/**
	 * Get the next versions.
	 * 
	 * @return iterable for all next versions
	 */
	Iterable<NodeGraphFieldContainer> getNextVersions();

	/**
	 * Set the next version.
	 * 
	 * @param container
	 */
	void setNextVersion(NodeGraphFieldContainer container);

	/**
	 * Check whether the field container has a previous version
	 * 
	 * @return true if the field container has a previous version
	 */
	boolean hasPreviousVersion();

	/**
	 * Get the previous version.
	 * 
	 * @return previous version or null
	 */
	NodeGraphFieldContainer getPreviousVersion();

	/**
	 * Make this container a clone of the given container. Property Vertices are reused.
	 *
	 * @param container
	 *            container
	 */
	void clone(NodeGraphFieldContainer container);

	/**
	 * Check whether this field container is the initial version for any branch.
	 * 
	 * @return true if it is the initial, false if not
	 */
	default boolean isInitial() {
		return isType(INITIAL);
	}

	/**
	 * Check whether this field container is the draft version for any branch.
	 * 
	 * @return true if it is the draft, false if not
	 */
	default boolean isDraft() {
		return isType(DRAFT);
	}

	/**
	 * Check whether this field container is the published version for any branch.
	 * 
	 * @return true if it is published, false if not
	 */
	default boolean isPublished() {
		return isType(PUBLISHED);
	}

	/**
	 * Check whether this field container has the given type for any branch.
	 * 
	 * @param type
	 * @return true if it matches the type, false if not
	 */
	boolean isType(ContainerType type);

	/**
	 * Check whether this field container is the initial version for the given branch.
	 * 
	 * @param branchUuid
	 *            branch Uuid
	 * @return true if it is the initial, false if not
	 */
	default boolean isInitial(String branchUuid) {
		return isType(INITIAL, branchUuid);
	}

	/**
	 * Check whether this field container is the draft version for the given branch.
	 * 
	 * @param branchUuid
	 *            branch Uuid
	 * @return true if it is the draft, false if not
	 */
	default boolean isDraft(String branchUuid) {
		return isType(DRAFT, branchUuid);
	}

	/**
	 * Check whether this field container is the published version for the given branch.
	 * 
	 * @param branchUuid
	 *            branch Uuid
	 * @return true if it is published, false if not
	 */
	default boolean isPublished(String branchUuid) {
		return isType(PUBLISHED, branchUuid);
	}

	/**
	 * Check whether this field container has the given type in the given branch.
	 * 
	 * @param type
	 * @param branchUuid
	 * @return true if it matches the type, false if not
	 */
	boolean isType(ContainerType type, String branchUuid);

	/**
	 * Get the branch Uuids for which this container is the container of given type.
	 * 
	 * @param type
	 *            type
	 * @return set of branch Uuids (may be empty, but never null)
	 */
	Set<String> getBranches(ContainerType type);

	/**
	 * Compare the container values of both containers and return a list of differences.
	 * 
	 * @param container
	 */
	List<FieldContainerChange> compareTo(NodeGraphFieldContainer container);

	/**
	 * Compare the values of this container with the values of the given fieldmap and return a list of detected differences.
	 * 
	 * @param fieldMap
	 * @return
	 */
	List<FieldContainerChange> compareTo(FieldMap fieldMap);

	@Override
	HibSchemaVersion getSchemaContainerVersion();

	/**
	 * Get all micronode fields that have a micronode using the given microschema container version.
	 * 
	 * @param version
	 *            microschema container version
	 * @return list of micronode fields
	 */
	List<MicronodeGraphField> getMicronodeFields(HibMicroschemaVersion version);

	/**
	 * Get all micronode list fields that have at least one micronode using the given microschema container version.
	 * 
	 * @param version
	 *            microschema container version
	 * @return list of micronode list fields
	 */
	Result<MicronodeGraphFieldList> getMicronodeListFields(HibMicroschemaVersion version);

	/**
	 * Return the ETag for the field container.
	 * 
	 * @param ac
	 * @return Generated entity tag
	 */
	String getETag(InternalActionContext ac);

	/**
	 * Determine the display field value by checking the schema and the referenced field and store it as a property.
	 */
	void updateDisplayFieldValue();

	/**
	 * Returns the segment field value of this container.
	 * 
	 * @return Determined segment field value or null if no segment field was specified or yet set
	 */
	String getSegmentFieldValue();

	/**
	 * Update the current segment field and increment any found postfix number.
	 */
	void postfixSegmentFieldValue();

	/**
	 * Return the URL field values for the container. The order of fields returned is the same order defined in the schema.
	 * 
	 * @return
	 */
	Stream<String> getUrlFieldValues();

	/**
	 * Traverse to the base node and build up the path to this container.
	 * 
	 * @param ac
	 * @return
	 */
	Path getPath(InternalActionContext ac);

	/**
	 * Return an iterator over the edges for the given type and branch.
	 * 
	 * @param type
	 * @param branchUuid
	 * @return
	 */
	Iterator<GraphFieldContainerEdge> getContainerEdge(ContainerType type, String branchUuid);

	/**
	 * Create the specific delete event.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	NodeMeshEventModel onDeleted(String branchUuid, ContainerType type);

	/**
	 * Create the specific create event.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	NodeMeshEventModel onCreated(String branchUuid, ContainerType type);

	/**
	 * Create the specific update event.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	NodeMeshEventModel onUpdated(String branchUuid, ContainerType type);

	/**
	 * Create the taken offline event.
	 *
	 * @param branchUuid
	 * @return
	 */
	NodeMeshEventModel onTakenOffline(String branchUuid);

	/**
	 * Create the publish event.
	 *
	 * @param branchUuid
	 * @return
	 */
	NodeMeshEventModel onPublish(String branchUuid);

	/**
	 * Transform the container into a version info object.
	 *
	 * @param ac
	 * @return
	 */
	VersionInfo transformToVersionInfo(InternalActionContext ac);

	/**
	 * A container is purgeable when it is not being utilized as draft, published or initial version in any branch.
	 *
	 * @return
	 */
	boolean isPurgeable();

	/**
	 * Check whether auto purge is enabled globally or for the schema of the container.
	 *
	 * @return
	 */
	boolean isAutoPurgeEnabled();

	/**
	 * Purge the container from the version history and ensure that the links between versions are consistent.
	 *
	 * @param bac
	 *            Action context for the deletion process
	 */
	void purge(BulkActionContext bac);

	/**
	 * Purge the container from the version without the use of a Bulk Action Context.
	 */
	default void purge() {
		purge(new DummyBulkActionContext());
	}

	/**
	 * Return all versions.
	 *
	 * @return
	 */
	Result<NodeGraphFieldContainer> versions();
}
