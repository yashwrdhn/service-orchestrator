package com.poc.orchestrator.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpressionEvaluator {
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private final ExpressionParser parser = new SpelExpressionParser();
    private ObjectMapper objectMapper;

    /**
     * Evaluate template string with variables
     * Replaces ${expression} with evaluated value
     */
    public String evaluate(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }

        EvaluationContext context = createContext(variables);

        StringBuffer result = new StringBuffer();
        Matcher matcher = EXPRESSION_PATTERN.matcher(template);

        while (matcher.find()) {
            String expressionString = matcher.group(1);
            try {
                Expression expression = parser.parseExpression(expressionString);
                Object value = expression.getValue(context);

                // Convert value to string representation
                String replacement;
                if (value == null) {
                    replacement = "null";
                } else if (value instanceof String) {
                    replacement = (String) value;
                } else {
                    try {
                        replacement = objectMapper.writeValueAsString(value);
                    } catch (JsonProcessingException e) {
                        replacement = value.toString();
                    }
                }

                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                log.warn("Failed to evaluate expression: {}", expressionString, e);
                matcher.appendReplacement(result, Matcher.quoteReplacement("${" + expressionString + "}"));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Create Spring EL evaluation context with variables
     */
    private EvaluationContext createContext(Map<String, Object> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Add variables to context
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        return context;
    }
}
