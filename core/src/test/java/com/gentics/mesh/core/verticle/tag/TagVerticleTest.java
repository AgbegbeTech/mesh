package com.gentics.mesh.core.verticle.tag;

import static com.gentics.mesh.core.data.model.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.rest.tag.request.TagUpdateRequest;
import com.gentics.mesh.core.rest.tag.response.TagListResponse;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.core.verticle.TagVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.JsonUtils;

public class TagVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagVerticle tagVerticle;

	@Autowired
	private TagService tagService;

	@Autowired
	private MeshNodeService contentService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return tagVerticle;
	}

	@Test
	public void testReadAllTags() throws Exception {

		// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
		Tag noPermTag = tagService.create();
		//noPermTag = data().addTag("NoPermEN", "NoPermDE");
		noPermTag.addProject(data().getProject());
		assertNotNull(noPermTag.getUuid());

		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags", 200, "OK");
		TagListResponse restResponse = JsonUtils.readValue(response, TagListResponse.class);
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals("The response did not contain the correct amount of items", data().getTags().size(), restResponse.getData().size());

		int perPage = 4;
		// Extra Tags + permitted tag
		int totalTags = data().getTags().size();
		int totalPages = (int) Math.ceil(totalTags / (double) perPage);
		List<TagResponse> allTags = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, TagListResponse.class);
			int expectedItemsCount = perPage;
			// The last page should only list 5 items
			if (page == 3) {
				expectedItemsCount = 4;
			}
			assertEquals("The expected item count for page {" + page + "} does not match", expectedItemsCount, restResponse.getData().size());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage());
			assertEquals("We requested page {" + page + "} but got a metainfo with a different page back.", page, restResponse.getMetainfo()
					.getCurrentPage());
			assertEquals("The amount of total pages did not match the expected value. There are {" + totalTags + "} tags and {" + perPage
					+ "} tags per page", totalPages, restResponse.getMetainfo().getPageCount());
			assertEquals("The total tag count does not match.", totalTags, restResponse.getMetainfo().getTotalCount());

			allTags.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all users were loaded when loading all pages.", totalTags, allTags.size());

		// Verify that the no_perm_tag is not part of the response
		final String noPermTagUUID = noPermTag.getUuid();
		List<TagResponse> filteredUserList = allTags.parallelStream().filter(restTag -> restTag.getUuid().equals(noPermTagUUID))
				.collect(Collectors.toList());
		assertTrue("The no perm tag should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags?per_page=" + perPage + "&page=" + 0, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags?per_page=" + 0 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags?per_page=" + -1 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		perPage = 25;
		totalPages = (int) Math.ceil(totalTags / (double) perPage);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags?per_page=" + perPage + "&page=" + 4242, 200, "OK");
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":" + totalPages + ",\"total_count\":" + totalTags
				+ "}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUUID() throws Exception {

		Tag tag = data().getTag("red");
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 200, "OK");
		System.out.println(response);
		TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);
		test.assertTag(tag, restTag);
	}

	@Test
	public void testReadTagByUUIDWithSingleLanguage() throws Exception {

		Tag tag = data().getTag("vehicle");
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);
		assertNull("The returned tag should not have an german name property.", restTag.getProperty("displayName"));
		assertNotNull("The returned tag should have an english name property.", restTag.getProperty("displayName"));
		test.assertTag(tag, restTag);
	}

	@Test
	public void testReadTagByUUIDWithMultipleLanguages() throws Exception {

		Tag tag = data().getTag("vehicle");
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en,de", 200, "OK");
		TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);
		test.assertTag(tag, restTag);
		assertNotNull(restTag.getProperty("displayName"));
		//TODO verify that name is english
	}

	@Test
	public void testReadTagByUUIDWithoutPerm() throws Exception {

		Tag tag = data().getTag("vehicle");
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
		info.getRole().revokePermissions(tag, READ_PERM);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, tag.getUuid());
	}

	@Test
	public void testUpdateTagByUUID() throws Exception {

		Tag tag = data().getTag("vehicle");

		// Create an tag update request
		TagUpdateRequest request = new TagUpdateRequest();
		request.setUuid(tag.getUuid());
		request.addProperty("displayName", "new Name");

		// 1. Read the current tag in english
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		System.out.println(response);
		TagResponse tagResponse = JsonUtils.readValue(response, TagResponse.class);
		String name = tag.getDisplayName(data().getEnglish());
		assertNotNull("The name of the tag should be loaded.", name);
		String restName = tagResponse.getProperty("displayName");
		Thread.sleep(100000);
		assertNotNull("The english displayName should be listed in the rest response since we requested the english tag", restName);
		assertEquals(name, restName);

		// 2. Setup the request object
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		final String newName = "new Name";
		tagUpdateRequest.addProperty("displayName", newName);
		assertEquals(newName, tagUpdateRequest.getProperty("displayName"));

		// 3. Send the request to the server
		// TODO test with no ?lang query parameter
		String requestJson = JsonUtils.toJson(request);
		response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK", requestJson);
		test.assertTag(tag, JsonUtils.readValue(response, TagResponse.class));

		// 4. read the tag again and verify that it was changed
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		tagResponse = JsonUtils.readValue(response, TagResponse.class);
		assertEquals(request.getProperty("displayName"), tagResponse.getProperty("displayName"));
		test.assertTag(tag, JsonUtils.readValue(response, TagResponse.class));
	}

	@Test
	public void testUpdateTagByUUIDWithoutPerm() throws Exception {
		Tag tag = data().getTag("vehicle");

		info.getRole().revokePermissions(tag, UPDATE_PERM);

		// Create an tag update request
		TagUpdateRequest request = new TagUpdateRequest();
		request.setUuid(tag.getUuid());
		request.addProperty("name", "new Name");

		String requestJson = new ObjectMapper().writeValueAsString(request);
		String response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 403, "Forbidden", requestJson);
		expectMessageResponse("error_missing_perm", response, tag.getUuid());

		// read the tag again and verify that it was not changed
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		TagResponse tagUpdateRequest = JsonUtils.readValue(response, TagResponse.class);

		String name = tag.getName(data().getEnglish());
		assertEquals(name, tagUpdateRequest.getProperty("name"));
	}

	// Delete Tests
	@Test
	public void testDeleteTagByUUID() throws Exception {

		Tag tag = data().getTag("vehicle");
		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 200, "OK");
		expectMessageResponse("tag_deleted", response, tag.getUuid());
		assertNull("The tag should have been deleted", tagService.findByUUID(tag.getUuid()));
	}

	@Test
	public void testDeleteTagByUUIDWithoutPerm() throws Exception {

		Tag tag = data().getTag("vehicle");
		info.getRole().revokePermissions(tag, DELETE_PERM);

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, tag.getUuid());
		assertNotNull("The tag should not have been deleted", tagService.findByUUID(tag.getUuid()));
	}

}
