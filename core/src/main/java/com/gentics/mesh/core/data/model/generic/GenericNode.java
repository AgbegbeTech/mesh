package com.gentics.mesh.core.data.model.generic;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.mesh.core.data.model.Project;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.TPGenericNode;

/**
 * This class represents a basic mesh node. All models that make use of this model will automatically be able to be handled by the permission system.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class GenericNode extends AbstractPersistable {

	private static final long serialVersionUID = -7525642021064006664L;

	@RelatedTo(type = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUTGOING, elementClass = Project.class)
	protected Set<Project> projects = new HashSet<>();

	@RelatedTo(type = BasicRelationships.HAS_CREATOR, direction = Direction.OUTGOING, elementClass = User.class)
	protected User creator;

//	@RelatedToVia(type = BasicRelationships.IS_LOCKED, direction = Direction.OUTGOING, elementClass = Locked.class)
//	protected Locked locked;

	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	public Set<Project> getProjects() {
		return projects;
	}

	public boolean addProject(Project project) {
		return this.projects.add(project);
	}

	public boolean removeProject(Project project) {
		return this.projects.remove(project);
	}

//	public boolean isLocked() {
//		if (locked == null) {
//			return false;
//		} else {
//			return locked.isValidLock();
//		}
//	}

}