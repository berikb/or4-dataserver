package kz.tamur.or4.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import kz.tamur.or4.data.bind.Interface;

@Path("/interface")
public class InterfaceService {
	
	@GET
	@Produces({ MediaType.APPLICATION_JSON  + ";charset=utf-8"})
	@Path("/{interfaceId}")
	public Interface getConfig(
			@PathParam("interfaceId") String interfaceId,
			@Context HttpServletResponse response,
			@Context ServletContext servletContext
			) throws Exception {
		response.setHeader("Access-Control-Allow-Origin", "*");

		InterfaceManager manager = InterfaceManager.getInstance(servletContext);
		InterfaceConfig cfg = manager.getConfig(interfaceId);
		return cfg.getInterface(); 
	}
	
	@GET
	@Produces({ MediaType.APPLICATION_JSON  + ";charset=utf-8"})
	@Path("/")
	public Collection<Interface> getAllConfigs(
			@Context HttpServletResponse response,
			@Context ServletContext servletContext
			) throws Exception {
		response.setHeader("Access-Control-Allow-Origin", "*");

		Collection<Interface> res = new ArrayList<>();
		InterfaceManager manager = InterfaceManager.getInstance(servletContext);
		for (InterfaceConfig config : manager.getAllConfigs()) {
			res.add(config.getInterface());
		}
		return res; 
	}

	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.TEXT_HTML  + ";charset=utf-8"})
	public Response uploadFile(
		@FormDataParam("file") InputStream uploadedInputStream,
		@FormDataParam("file") FormDataContentDisposition fileDetail,
		@Context ServletContext servletContext
		) {

		try {
			InterfaceManager manager = InterfaceManager.getInstance(servletContext);
			File confFile = new File(manager.getConfigDir(), fileDetail.getFileName());
			writeToFile(uploadedInputStream, confFile);
			manager.addConfig(confFile);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			return Response.status(500).entity(sw.toString()).build();
		}

		return Response.status(200).entity("Интерфейс успешно загружен.").build();
	}

	// save uploaded file to new location
	private void writeToFile(InputStream uploadedInputStream, File uploadedFile) throws Exception {

		OutputStream out = new FileOutputStream(uploadedFile);
		int read = 0;
		byte[] bytes = new byte[1024];

		while ((read = uploadedInputStream.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}
		out.flush();
		out.close();
	}
}
