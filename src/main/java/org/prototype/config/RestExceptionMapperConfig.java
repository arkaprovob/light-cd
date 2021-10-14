package org.prototype.config;

import org.prototype.type.ErrorResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Objects;

@Provider
public class RestExceptionMapperConfig implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception throwable) {
        String errorMessage = Objects.isNull(throwable.getMessage()) ? throwable.toString() : throwable.getMessage();
        var response = new ErrorResponse(errorMessage, "500",
                "https://github.com/arkaprovob/light-cd/blob/master/README.md");
        return Response.ok().entity(response).status(500).build();
    }
}
