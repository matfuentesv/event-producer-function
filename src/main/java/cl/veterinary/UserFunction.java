package cl.veterinary;

import cl.veterinary.model.Rol;
import cl.veterinary.model.User;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.HashMap;
import java.util.Map;

public class UserFunction {

    private final String eventGridTopicEndpoint = "https://user-role-events-topic.eastus2-1.eventgrid.azure.net/api/events";
    private final String eventGridTopicKey = "FWObSItpv8zDesKQBo5bEYvYKWCGZ9yq1wR82E7DmSm5LHhmGaTkJQQJ99BDACHYHv6XJ3w3AAABAZEGBFN8";

    private final EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
            .endpoint(eventGridTopicEndpoint)
            .credential(new AzureKeyCredential(eventGridTopicKey))
            .buildEventGridEventPublisherClient();

    private final ObjectMapper mapper = new ObjectMapper();

    @FunctionName("UserCrudFunction")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE}, authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<String> request,
            final ExecutionContext context) {

        try {
            HttpMethod method = request.getHttpMethod();

            return switch (method) {
                case POST -> insertUser(request, context);
                case PUT -> updateUser(request, context);
                case DELETE -> deleteUser(request, context);
                case GET -> getUser(request, context);
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

    private HttpResponseMessage insertUser(HttpRequestMessage<String> request, ExecutionContext context) throws Exception {
        User user = mapper.readValue(request.getBody(), User.class);
        sendEvent(user, "CREATE");
        return request.createResponseBuilder(HttpStatus.OK)
                .body("Usuario creado y evento enviado correctamente.")
                .build();
    }


    private HttpResponseMessage updateUser(HttpRequestMessage<String> request, ExecutionContext context) throws Exception {
        User user = mapper.readValue(request.getBody(), User.class);
        sendEvent(user, "UPDATE");
        return request.createResponseBuilder(HttpStatus.OK)
                .body("Usuario actualizado y evento enviado correctamente.")
                .build();
    }

    private HttpResponseMessage deleteUser(HttpRequestMessage<String> request, ExecutionContext context) throws Exception {
        User user = mapper.readValue(request.getBody(), User.class);
        sendEvent(user, "DELETE");
        return request.createResponseBuilder(HttpStatus.OK)
                .body("Usuario eliminado y evento enviado correctamente.")
                .build();
    }

    private HttpResponseMessage getUser(HttpRequestMessage<String> request, ExecutionContext context) {
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
                .body("Consulta de usuario enviada correctamente.")
                .build();
    }


    private void sendEvent(User user, String operation) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("id", user.getId());
        eventData.put("nombre", user.getNombre());
        eventData.put("apellidoPaterno", user.getApellidoPaterno());
        eventData.put("apellidoMaterno", user.getApellidoMaterno());
        eventData.put("rut", user.getRut());
        eventData.put("direccion", user.getDireccion());
        eventData.put("celular", user.getCelular());
        eventData.put("email", user.getEmail());
        eventData.put("password", user.getPassword());
        eventData.put("activo", user.getActivo());
        eventData.put("rol", user.getRol());
        eventData.put("operation", operation);

        EventGridEvent event = new EventGridEvent(
                "/veterinary/usuario",
                "Usuario." + operation,
                BinaryData.fromObject(eventData),
                "1.0"
        );
        client.sendEvent(event);
    }

    private void sendSimpleEvent(Map<String, Object> data, String operation) {
        EventGridEvent event = new EventGridEvent(
                "/veterinary/usuario",
                "Usuario." + operation,
                BinaryData.fromObject(data),
                "1.0"
        );
        client.sendEvent(event);
    }

}
