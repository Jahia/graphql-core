package org.jahia.modules.graphql.provider.dxm.config;

import graphql.kickstart.execution.input.GraphQLBatchedInvocationInput;
import graphql.kickstart.execution.input.GraphQLSingleInvocationInput;
import graphql.kickstart.servlet.input.BatchInputPreProcessResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequestOperationLimitPreProcessor}: the bound on how many operations one request may submit.
 *
 * <p>The limit is held statically, since that is how the configuration reaches the pre-processor, so each test states
 * the value it exercises and the shipped default is restored around every one of them. The class therefore does not
 * depend on the order it runs in, nor on what another test leaves in that field.
 *
 * <p>Both tests under "the shipped default" read {@link RequestOperationLimitPreProcessor#DEFAULT_OPERATION_LIMIT}
 * rather than a number of their own, so they are what turns this suite red if that default ever stops bounding a
 * request.
 */
public class RequestOperationLimitPreProcessorTest {

    private final RequestOperationLimitPreProcessor preProcessor = new RequestOperationLimitPreProcessor();
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Before
    public void startFromTheShippedDefault() {
        RequestOperationLimitPreProcessor.updateOperationLimit(
                RequestOperationLimitPreProcessor.DEFAULT_OPERATION_LIMIT);
    }

    @After
    public void restoreTheShippedDefault() {
        RequestOperationLimitPreProcessor.updateOperationLimit(
                RequestOperationLimitPreProcessor.DEFAULT_OPERATION_LIMIT);
    }

    private static GraphQLBatchedInvocationInput requestOf(int operations) {
        GraphQLBatchedInvocationInput input = mock(GraphQLBatchedInvocationInput.class);
        List<GraphQLSingleInvocationInput> inputs =
                Collections.nCopies(operations, mock(GraphQLSingleInvocationInput.class));
        when(input.getInvocationInputs()).thenReturn(inputs);
        return input;
    }

    private BatchInputPreProcessResult preProcess(int operations) {
        return preProcessor.preProcessBatch(requestOf(operations), request, response);
    }

    // --- a request within the bound is executed, and executed whole ---

    @Test
    public void shouldAcceptASingleOperation() {
        RequestOperationLimitPreProcessor.updateOperationLimit(5);
        assertTrue(preProcess(1).isExecutable());
    }

    @Test
    public void shouldAcceptARequestExactlyAtTheLimit() {
        RequestOperationLimitPreProcessor.updateOperationLimit(5);
        assertTrue(preProcess(5).isExecutable());
    }

    @Test
    public void shouldPassOnEveryOperationOfAnAcceptedRequest() {
        // The bound decides whether a request runs; it never decides which of its operations do.
        RequestOperationLimitPreProcessor.updateOperationLimit(5);
        GraphQLBatchedInvocationInput submitted = requestOf(5);

        BatchInputPreProcessResult result = preProcessor.preProcessBatch(submitted, request, response);

        assertTrue(result.isExecutable());
        assertSame(submitted, result.getBatchedInvocationInput());
    }

    // --- a request over the bound is refused, as a whole ---

    @Test
    public void shouldRefuseARequestOneOperationOverTheLimit() {
        RequestOperationLimitPreProcessor.updateOperationLimit(5);
        assertFalse(preProcess(6).isExecutable());
    }

    @Test
    public void shouldRefuseAsABadRequest() {
        RequestOperationLimitPreProcessor.updateOperationLimit(5);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, preProcess(6).getStatusCode());
    }

    @Test
    public void shouldReportWhatWasSubmittedAndWhatIsAllowed() {
        RequestOperationLimitPreProcessor.updateOperationLimit(5);

        String message = preProcess(6).getStatusMessage();

        assertTrue(message, message.contains("6"));
        assertTrue(message, message.contains("5"));
    }

    @Test
    public void shouldRefuseALargeRequestWhateverItsOperationsWouldCost() {
        // The count is the whole measure here: these operations are never analysed, so a request of trivial ones is
        // bounded exactly like a request of expensive ones.
        RequestOperationLimitPreProcessor.updateOperationLimit(5);
        assertFalse(preProcess(5000).isExecutable());
    }

    // --- the bound is configurable, including off ---

    @Test
    public void shouldApplyNoBoundWhenSetToZero() {
        RequestOperationLimitPreProcessor.updateOperationLimit(0);
        assertTrue(preProcess(5000).isExecutable());
    }

    @Test
    public void shouldApplyANewLimitToTheNextRequest() {
        RequestOperationLimitPreProcessor.updateOperationLimit(5);
        assertFalse(preProcess(6).isExecutable());

        RequestOperationLimitPreProcessor.updateOperationLimit(10);
        assertTrue(preProcess(6).isExecutable());
    }

    // --- the shipped default bounds a request on its own, with no configuration read ---

    @Test
    public void shouldBoundARequestByDefault() {
        assertFalse(preProcess(RequestOperationLimitPreProcessor.DEFAULT_OPERATION_LIMIT + 1).isExecutable());
    }

    @Test
    public void shouldAcceptARequestAtTheShippedDefault() {
        assertTrue(preProcess(RequestOperationLimitPreProcessor.DEFAULT_OPERATION_LIMIT).isExecutable());
    }
}
