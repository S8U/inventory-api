package com.github.s8u.inventory.config;

import com.github.s8u.inventory.exception.ApiErrorCodes;
import com.github.s8u.inventory.exception.ErrorCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

@Configuration
public class SwaggerConfig {

    private static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";
    private static final String EXAMPLE_SKU = "WTR-500";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory API")
                        .description("상품 입고, 출고, 재고 조회 API")
                        .version("1.0.0"));
    }

    @Bean
    public OpenApiCustomizer problemDetailSchemaCustomizer() {
        return openApi -> openApi.getComponents().addSchemas(PROBLEM_DETAIL_SCHEMA, problemDetailSchema());
    }

    /**
     * API마다 @ApiErrorCodes에 적은 에러 코드로 에러 응답을 문서에 적는다
     *
     * 같은 상태의 에러 코드는 한 응답에 모으고, 에러 코드마다 실제 응답과 같은 모양의 예시를 붙인다.
     * 처리하지 못한 예외(INTERNAL_ERROR)는 어느 API에서나 날 수 있어 모든 API에 넣는다.
     */
    @Bean
    public OperationCustomizer errorResponseCustomizer() {
        return (operation, handlerMethod) -> {
            Set<ErrorCode> errorCodes = EnumSet.of(ErrorCode.INTERNAL_ERROR);
            ApiErrorCodes apiErrorCodes = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);
            if (apiErrorCodes != null)
                errorCodes.addAll(List.of(apiErrorCodes.value()));

            String path = examplePath(handlerMethod);
            errorCodes.stream()
                    .collect(Collectors.groupingBy(errorCode -> errorCode.getStatus().value(), TreeMap::new, Collectors.toList()))
                    .forEach((status, codes) -> operation.getResponses()
                            .addApiResponse(String.valueOf(status), errorResponse(codes, path)));

            return operation;
        };
    }

    /**
     * 에러 응답 스키마
     *
     * ProblemDetail 클래스를 그대로 스키마로 쓰면 code가 properties라는 객체 안에 그려진다.
     * 실제 응답은 code가 최상위 필드라서 스키마를 직접 만든다.
     * type은 Spring 7이 기본으로 넣지 않아 뺐다 (RFC 9457은 type이 없으면 about:blank로 본다).
     * 필드 예시는 넣지 않는다. 모든 에러 응답이 이 스키마를 써서, 넣으면 어느 응답이든 같은 예시가 보인다.
     */
    private Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .description("에러 응답 (RFC 9457 Problem Details)")
                .addProperty("title", new StringSchema().description("HTTP 상태 이름"))
                .addProperty("status", new IntegerSchema().description("HTTP 상태 코드"))
                .addProperty("detail", new StringSchema().description("에러 설명"))
                .addProperty("instance", new StringSchema().description("요청 경로"))
                .addProperty("code", new StringSchema()
                        .description("에러 코드")
                        ._enum(Arrays.stream(ErrorCode.values()).map(Enum::name).toList()));
    }

    private ApiResponse errorResponse(List<ErrorCode> errorCodes, String path) {
        String description = errorCodes.stream()
                .map(errorCode -> "%s: %s".formatted(errorCode.name(), errorCode.getDescription()))
                .collect(Collectors.joining(", "));

        MediaType mediaType = new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_DETAIL_SCHEMA));
        errorCodes.forEach(errorCode -> mediaType.addExamples(errorCode.name(), new Example().value(exampleBody(errorCode, path))));

        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(APPLICATION_PROBLEM_JSON_VALUE, mediaType));
    }

    private Map<String, Object> exampleBody(ErrorCode errorCode, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", errorCode.getStatus().getReasonPhrase());
        body.put("status", errorCode.getStatus().value());
        body.put("detail", errorCode.getMessage());
        body.put("instance", path);
        body.put("code", errorCode.name());
        return body;
    }

    // 예시의 instance(요청 경로). 경로 변수 {sku}에는 예시 SKU를 넣는다
    private String examplePath(HandlerMethod handlerMethod) {
        return (mappingPath(handlerMethod.getBeanType()) + mappingPath(handlerMethod.getMethod()))
                .replace("{sku}", EXAMPLE_SKU);
    }

    private String mappingPath(AnnotatedElement element) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
        if (mapping == null || mapping.path().length == 0)
            return "";

        return mapping.path()[0];
    }

}
