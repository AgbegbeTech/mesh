package com.gentics.mesh.core.action.impl;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Tx;

/**
 * @see DAOActionContext
 */
public class DAOActionContextImpl implements DAOActionContext {

	private final Tx tx;
	private final HibProject project;
	private final Branch branch;
	private final Object parent;

	private InternalActionContext ac;

	public DAOActionContextImpl(Tx tx, InternalActionContext ac, Object parent) {
		this.tx = tx;
		this.ac = ac;
		this.project = ac.getProject();
		if (project != null) {
			this.branch = ac.getBranch(project);
		} else {
			this.branch = null;
		}
		this.parent = parent;
	}

	@Override
	public Tx tx() {
		return tx;
	}

	@Override
	public InternalActionContext ac() {
		return ac;
	}

	@Override
	public HibProject project() {
		return project;
	}

	@Override
	public <T> T parent() {
		return (T) parent;
	}

	@Override
	public Branch branch() {
		return branch;
	}
}
