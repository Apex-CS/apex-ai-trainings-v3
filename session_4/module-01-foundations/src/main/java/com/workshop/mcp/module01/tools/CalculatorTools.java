package com.workshop.mcp.module01.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Calculator MCP Tools — Module 01 Foundations.
 *
 * <p>Demonstrates basic @Tool + @ToolParam usage. Spring AI uses these annotations
 * to auto-generate JSON Schema descriptors that the MCP Server publishes in the
 * tools/list response so LLMs know how to call each tool correctly.
 */
@Service
public class CalculatorTools {

    @Tool(description = "Adds two numbers and returns the sum")
    public double add(
            @ToolParam(description = "First operand") double a,
            @ToolParam(description = "Second operand") double b) {
        return a + b;
    }

    @Tool(description = "Subtracts the subtrahend from the minuend and returns the difference")
    public double subtract(
            @ToolParam(description = "Minuend (number to subtract from)") double minuend,
            @ToolParam(description = "Subtrahend (number to subtract)") double subtrahend) {
        return minuend - subtrahend;
    }

    @Tool(description = "Multiplies two numbers and returns the product")
    public double multiply(
            @ToolParam(description = "First factor") double a,
            @ToolParam(description = "Second factor") double b) {
        return a * b;
    }

    /**
     * Divides dividend by divisor.
     *
     * <p>Throws {@link ArithmeticException} when divisor is zero. Spring AI catches
     * the exception and returns a {@code CallToolResult} with {@code isError:true},
     * placing the exception message in the content body. This demonstrates the MCP
     * pattern: tool-level errors belong in the content body with isError flag, NOT
     * in JSON-RPC error objects.
     */
    @Tool(description = "Divides dividend by divisor. Returns isError:true if divisor is zero.")
    public double divide(
            @ToolParam(description = "Dividend (numerator)") double dividend,
            @ToolParam(description = "Divisor (denominator) — must not be zero") double divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return dividend / divisor;
    }
}
