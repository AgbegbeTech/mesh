package com.gentics.cailun.core.data.service.tag;

import io.vertx.ext.apex.Session;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;

import org.neo4j.graphdb.Transaction;

import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.content.TransformationInfo;
import com.gentics.cailun.core.rest.common.response.AbstractTagContainerModel;
import com.gentics.cailun.core.rest.tag.response.TagResponse;

public class TagTraversalConsumer implements Consumer<Tag> {

	private TransformationInfo info;
	private int currentDepth;
	private AbstractTagContainerModel tagContainer;
	private Set<ForkJoinTask<Void>> tasks;

	public TagTraversalConsumer(TransformationInfo info, int currentDepth, AbstractTagContainerModel tagContainer, Set<ForkJoinTask<Void>> tasks) {
		this.info = info;
		this.currentDepth = currentDepth;
		this.tagContainer = tagContainer;
		this.tasks = tasks;
	}

	@Override
	public void accept(Tag tag) {
		String currentUuid = tag.getUuid();
		Session session = info.getRoutingContext().session();
		session.hasPermission(new CaiLunPermission(tag, PermissionType.READ).toString(), handler -> {
			if (handler.result()) {
				try (Transaction tx = info.getGraphDb().beginTx()) {
					Tag loadedTag = info.getNeo4jTemplate().fetch(tag);
					TagResponse currentRestTag = (TagResponse) info.getObject(currentUuid);
					if (currentRestTag == null) {
						currentRestTag = new TagResponse();
						/* info.addTag(currentUuid, currentRestTag); */
						TagTransformationTask subTask = new TagTransformationTask(loadedTag, info, currentRestTag, currentDepth + 1);
						tasks.add(subTask.fork());

						tx.success();
					}
					tagContainer.getTags().add(currentRestTag);
				}

				Collections.sort(tagContainer.getTags(), new UuidRestModelComparator<AbstractTagContainerModel>());

			}
		});

	}

}
