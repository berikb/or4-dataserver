package kz.tamur.or4.data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

@Path("/data")
public class DataService {

	@GET
	@Produces({ MediaType.APPLICATION_JSON  + ";charset=utf-8"})
	@Path("/{interfaceId}/{compId}")
	public Object get(
			@PathParam("interfaceId") String interfaceId,
			@PathParam("compId") String compId,
			@Context UriInfo uriInfo,
			@Context HttpServletResponse response,
			@Context ServletContext servletContext
			) throws Exception {
		response.setHeader("Access-Control-Allow-Origin", "*");

		InterfaceManager manager = InterfaceManager.getInstance(servletContext);
		InterfaceConfig cfg = manager.getConfig(interfaceId);
		return cfg.getData(compId, uriInfo.getQueryParameters());
	}
	
	@GET
	@Produces({ MediaType.APPLICATION_JSON  + ";charset=utf-8"})
	@Path("/{interfaceId}")
	public Object getAll(
			@PathParam("interfaceId") String interfaceId,
			@Context HttpHeaders headers,
			@Context UriInfo uriInfo,
			@Context HttpServletResponse response,
			@Context ServletContext servletContext
			) {
		response.setHeader("Access-Control-Allow-Origin", "*");

		try {
			// Получаем текущее состояние интерфейса из куки
			Cookie stateCookie = headers.getCookies().get(interfaceId);
			JsonNode stateJson = null;
			if (stateCookie != null) {
				ObjectMapper mapper = new ObjectMapper();
				stateJson = mapper.readTree(stateCookie.getValue());
			}
			
			InterfaceManager manager = InterfaceManager.getInstance(servletContext);
			InterfaceConfig cfg = manager.getConfig(interfaceId);
			
			return cfg.getData(uriInfo.getQueryParameters(), stateJson);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			return Response.status(500).entity(sw.toString()).build();
		}
	}

	@POST
	@Consumes({ MediaType.APPLICATION_JSON  + ";charset=utf-8" })
	@Produces({ MediaType.APPLICATION_JSON  + ";charset=utf-8"})
	@Path("/{interfaceId}")
	public Response set(
			String jsonString,
			@PathParam("interfaceId") String interfaceId,
			@Context HttpHeaders headers,
			@Context UriInfo uriInfo,
			@Context HttpServletResponse response,
			@Context ServletContext servletContext
			) throws Exception {
		
		response.setHeader("Access-Control-Allow-Origin", "*");

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(jsonString);

		InterfaceManager manager = InterfaceManager.getInstance(servletContext);
		InterfaceConfig cfg = manager.getConfig(interfaceId);
		ObjectNode newStateJson = mapper.createObjectNode();
		Object res = cfg.setData(root, uriInfo.getQueryParameters(), newStateJson);
		
		// Получаем текущее состояние интерфейса из куки
		Cookie stateCookie = headers.getCookies().get(interfaceId);
		ObjectNode oldStateJson = stateCookie != null ? (ObjectNode)mapper.readTree(stateCookie.getValue()) : mapper.createObjectNode();

		boolean updateCookie = false;
		for (Iterator<String> nameIt = newStateJson.getFieldNames(); nameIt.hasNext();) {
			String name = nameIt.next();
			oldStateJson.put(name, newStateJson.get(name));
			updateCookie = true;
		}
		
		ResponseBuilder responseBuilder = Response.ok(res);
		if (updateCookie) {
			responseBuilder.cookie(new NewCookie(interfaceId, oldStateJson.toString()));
		}
		return responseBuilder.build();
	}

	@POST
	@Consumes({ MediaType.APPLICATION_JSON  + ";charset=utf-8" })
	@Produces({ MediaType.APPLICATION_JSON  + ";charset=utf-8"})
	@Path("/{interfaceId}/{actionId}")
	public Response exec(
			String jsonString,
			@PathParam("interfaceId") String interfaceId,
			@PathParam("actionId") String actionId,
			@Context HttpHeaders headers,
			@Context UriInfo uriInfo,
			@Context HttpServletResponse response,
			@Context ServletContext servletContext
			) throws Exception {
		
		response.setHeader("Access-Control-Allow-Origin", "*");

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(jsonString);

		InterfaceManager manager = InterfaceManager.getInstance(servletContext);
		InterfaceConfig cfg = manager.getConfig(interfaceId);
		Object res = cfg.execute(actionId, root);

		ResponseBuilder responseBuilder = Response.ok(res);
		return responseBuilder.build();
	}
}
