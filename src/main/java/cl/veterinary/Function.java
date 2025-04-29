package cl.veterinary;

import cl.veterinary.model.Rol;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.HashMap;
import java.util.Map;

public class Function {

    private final String eventGridTopicEndpoint = "https://user-role-events-topic.eastus2-1.eventgrid.azure.net/api/events";
    private final String eventGridTopicKey = "FWObSItpv8zDesKQBo5bEYvYKWCGZ9yq1wR82E7DmSm5LHhmGaTkJQQJ99BDACHYHv6XJ3w3AAABAZEGBFN8";

    private final EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
            .endpoint(eventGridTopicEndpoint)
            .credential(new AzureKeyCredential(eventGridTopicKey))
            .buildEventGridEventPublisherClient();

    private final ObjectMapper mapper = new ObjectMapper();

    @FunctionName("RoleCrudFunction")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE}, authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<String> request,
            final ExecutionContext context) {

        try {
            HttpMethod method = request.getHttpMethod();

            return switch (method) {
                case POST -> insertRol(request, context);
                case PUT -> updateRol(request, context);
                case DELETE -> deleteRol(request, context);
                case GET -> getRol(request, context);
                default -> request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("Método HTTP no soportado")
                        .build();
            };
        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno: " + e.getMessage())
                    .build();
        }
    }

    private HttpResponseMessage insertRol(HttpRequestMessage<String> request, ExecutionContext context) throws Exception {
        Rol rol = mapper.readValue(request.getBody(), Rol.class);
        sendEvent(rol, "CREATE");
        return request.createResponseBuilder(HttpStatus.OK)
                .body("Rol creado y evento enviado correctamente.")
                .build();
    }

    private HttpResponseMessage updateRol(HttpRequestMessage<String> request, ExecutionContext context) throws Exception {
        Rol rol = mapper.readValue(request.getBody(), Rol.class);
        sendEvent(rol, "UPDATE");
        return request.createResponseBuilder(HttpStatus.OK)
                .body("Rol actualizado y evento enviado correctamente.")
                .build();
    }

    private HttpResponseMessage deleteRol(HttpRequestMessage<String> request, ExecutionContext context) throws Exception {
        Rol rol = mapper.readValue(request.getBody(), Rol.class);
        sendEvent(rol, "DELETE");
        return request.createResponseBuilder(HttpStatus.OK)
                .body("Rol eliminado y evento enviado correctamente.")
                .build();
    }

    private HttpResponseMessage getRol(HttpRequestMessage<String> request, ExecutionContext context) {
        String id = request.getQueryParameters().get("id");
        if (id == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Debe especificar el parámetro id")
                    .build();
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        sendSimpleEvent(data, "GET");
        return request.createResponseBuilder(HttpStatus.OK)
                .body("Consulta de rol enviada correctamente.")
                .build();
    }

    private void sendEvent(Rol rol, String operation) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("id", rol.getId());
        eventData.put("descripcion", rol.getDescripcion());
        eventData.put("operation", operation);

        EventGridEvent event = new EventGridEvent(
                "/veterinary/rol",
                "Rol." + operation,
                BinaryData.fromObject(eventData),
                "1.0"
        );
        client.sendEvent(event);
    }

    private void sendSimpleEvent(Map<String, Object> data, String operation) {
        EventGridEvent event = new EventGridEvent(
                "/veterinary/rol",
                "Rol." + operation,
                BinaryData.fromObject(data),
                "1.0"
        );
        client.sendEvent(event);
    }
}
