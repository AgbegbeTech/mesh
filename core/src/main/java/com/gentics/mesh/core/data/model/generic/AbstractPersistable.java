package com.gentics.mesh.core.data.model.generic;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.mesh.core.data.model.tinkerpop.TPAbstractPersistable;
import com.tinkerpop.blueprints.Vertex;

/**
 * Abstract class for all node entities.
 * 
 * @author johannes2
 *
 */
public abstract class AbstractPersistable implements Serializable {
	private static final long serialVersionUID = -3244769429406745303L;

	@Autowired
	protected Neo4jTemplate neo4jTemplate;

	/**
	 * The mandatory neo4j graph id for this object.
	 */
	@GraphId
	private Long id;

	/**
	 * The uuid of the object. A transaction event handler is being used in order to generate and verify the integrity of uuids.
	 */
	private String uuid;

	/**
	 * Return the id.
	 * 
	 * @return
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Return the uuid for the object.
	 * 
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Check whether the object was not yet saved.
	 * 
	 * @return true, when the object was not yet saved. Otherwise false.
	 */
	public boolean isNew() {
		return null == getId();
	}

	@Override
	public int hashCode() {
		int hashCode = 17;

		hashCode += isNew() ? 0 : getId().hashCode() * 31;

		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {

		if (null == obj) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!getClass().equals(obj.getClass())) {
			return false;
		}

		AbstractPersistable that = (AbstractPersistable) obj;

		return null == this.getId() ? false : this.getId().equals(that.getId());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "#" + getUuid();
	}
}
