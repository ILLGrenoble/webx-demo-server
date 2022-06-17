package eu.ill.webx.controllers;

import eu.ill.webx.controllers.dto.ConfigurationDto;
import eu.ill.webx.services.ConfigurationService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/configuration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigurationController {

    public ConfigurationController() {
    }

    @GET
    public Response configuration() {
        return Response.status(200).entity(new ConfigurationDto(ConfigurationService.instance().getConfiguration())).build();
    }
}
