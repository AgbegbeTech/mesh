package com.gentics.mesh.test;

import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RestAssert;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

@ContextConfiguration(classes = { SpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractDBTest {

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	private DemoDataProvider dataProvider;

	@Autowired
	protected MeshSpringConfiguration springConfig;

	@Autowired
	protected Database db;

	@Autowired
	protected DatabaseService databaseService;

	@Autowired
	protected RestAssert test;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		JsonUtil.debugMode = true;
	}

	public void setupData() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		dataProvider.setup(1);
	}

	public SchemaContainer schemaContainer(String key) {
		SchemaContainer container = dataProvider.getSchemaContainer(key);
		container.reload();
		return container;

	}

	public UserInfo getUserInfo() {
		return dataProvider.getUserInfo();
	}

	public Map<String, ? extends Tag> tags() {
		return dataProvider.getTags();
	}

	public Tag tag(String key) {
		Tag tag = dataProvider.getTag(key);
		return tag;
	}

	public TagFamily tagFamily(String key) {
		TagFamily family = dataProvider.getTagFamily(key);
		return family;
	}

	public Project project() {
		Project project = dataProvider.getProject();
		project.reload();
		return project;
	}

	public Node content(String key) {
		Node node = dataProvider.getContent(key);
		return node;
	}

	public Node folder(String key) {
		Node node = dataProvider.getFolder(key);
		return node;
	}

	public Map<String, User> users() {
		return dataProvider.getUsers();
	}

	public Map<String, Role> roles() {
		return dataProvider.getRoles();
	}

	public Map<String, TagFamily> tagFamilies() {
		return dataProvider.getTagFamilies();
	}

	public Map<String, Group> groups() {
		return dataProvider.getGroups();
	}

	public Map<String, SchemaContainer> schemaContainers() {
		return dataProvider.getSchemaContainers();
	}

	public int getNodeCount() {
		return dataProvider.getNodeCount();
	}

	public Language english() {
		return dataProvider.getEnglish();
	}

	public Language german() {
		return dataProvider.getGerman();
	}

	public User user() {
		User user = dataProvider.getUserInfo().getUser();
		return user;
	}

	public String password() {
		return dataProvider.getUserInfo().getPassword();
	}

	public Group group() {
		Group group = dataProvider.getUserInfo().getGroup();
		group.reload();
		return group;
	}

	public Role role() {
		Role role = dataProvider.getUserInfo().getRole();
		role.reload();
		return role;
	}

	public MeshRoot meshRoot() {
		return dataProvider.getMeshRoot();
	}

	public Node content() {
		Node content = dataProvider.getContent("news overview");
		return content;
	}

	public MeshAuthUser getRequestUser() {
		return dataProvider.getUserInfo().getUser().getImpl().reframe(MeshAuthUserImpl.class);
	}

	public SchemaContainer getSchemaContainer() {
		SchemaContainer container = dataProvider.getSchemaContainer("content");
		return container;
	}

	protected String getJson(Node node) throws InterruptedException {
		RoutingContext rc = getMockedRoutingContext("lang=en");
		ActionContext ac = ActionContext.create(rc);
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> reference = new AtomicReference<>();
		node.transformToRest(ac, rh -> {
			NodeResponse response = rh.result();
			reference.set(JsonUtil.toJson(response));
			assertNotNull(response);
			latch.countDown();
		});
		failingLatch(latch);
		return reference.get();
	}

	protected RoutingContext getMockedRoutingContext(String query) {
		try (Trx tx = db.trx()) {
			User user = dataProvider.getUserInfo().getUser();
			Map<String, Object> map = new HashMap<>();
			RoutingContext rc = mock(RoutingContext.class);
			Session session = mock(Session.class);
			HttpServerRequest request = mock(HttpServerRequest.class);
			when(request.query()).thenReturn(query);

			MeshAuthUserImpl requestUser = tx.getGraph().frameElement(user.getElement(), MeshAuthUserImpl.class);
			when(rc.data()).thenReturn(map);
			when(rc.request()).thenReturn(request);
			when(rc.session()).thenReturn(session);
			JsonObject principal = new JsonObject();
			principal.put("uuid", user.getUuid());
			when(rc.user()).thenReturn(requestUser);
			return rc;
		}
	}

}
