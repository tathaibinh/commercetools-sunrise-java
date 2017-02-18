package com.commercetools.sunrise.myaccount.authentication;

import com.commercetools.sunrise.controllers.AbstractSphereRequestExecutor;
import com.commercetools.sunrise.framework.hooks.HookRunner;
import com.commercetools.sunrise.framework.hooks.actions.CustomerSignedInActionHook;
import com.commercetools.sunrise.framework.hooks.events.CustomerSignInResultLoadedHook;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.customers.CustomerSignInResult;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.customers.commands.CustomerSignInCommand;
import io.sphere.sdk.expansion.ExpansionPathContainer;
import play.libs.concurrent.HttpExecution;

import javax.annotation.Nullable;
import java.util.concurrent.CompletionStage;

public abstract class AbstractCustomerSignInExecutor extends AbstractSphereRequestExecutor {

    protected AbstractCustomerSignInExecutor(final SphereClient sphereClient, final HookRunner hookRunner) {
        super(sphereClient, hookRunner);
    }

    protected final CompletionStage<CustomerSignInResult> executeRequest(final CustomerCreateCommand request) {
        return executeRequest(request, request);
    }

    protected final CompletionStage<CustomerSignInResult> executeRequest(final CustomerSignInCommand request) {
        return executeRequest(request, null);
    }

    private CompletionStage<CustomerSignInResult> executeRequest(final SphereRequest<CustomerSignInResult> request, @Nullable final ExpansionPathContainer<CustomerSignInResult> expansionPathContainer) {
        return getSphereClient().execute(request)
                .thenComposeAsync(result -> CustomerSignedInActionHook.runHook(getHookRunner(), result, expansionPathContainer)
                                .thenApplyAsync(updatedResult -> {
                                    CustomerSignInResultLoadedHook.runHook(getHookRunner(), updatedResult);
                                    return updatedResult;
                                }, HttpExecution.defaultContext()),
                        HttpExecution.defaultContext());
    }
}