package com.commercetools.sunrise.common.template.engine;

import com.commercetools.sunrise.cms.CmsPage;
import com.commercetools.sunrise.cms.CmsService;
import com.commercetools.sunrise.contexts.UserLanguage;
import com.commercetools.sunrise.common.pages.PageContent;
import com.commercetools.sunrise.common.pages.PageData;
import com.commercetools.sunrise.common.pages.PageDataFactory;
import com.commercetools.sunrise.framework.hooks.RequestHookRunner;
import com.commercetools.sunrise.framework.hooks.consumers.PageDataReadyHook;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.libs.concurrent.HttpExecution;
import play.twirl.api.Content;
import play.twirl.api.Html;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class HtmlTemplateRenderer implements TemplateRenderer {

    private static final Logger pageDataLoggerAsJson = LoggerFactory.getLogger(PageData.class.getName() + "Json");
    private static final ObjectMapper objectMapper = createObjectMapper();

    private final UserLanguage userLanguage;
    private final PageDataFactory pageDataFactory;
    private final RequestHookRunner hookRunner;
    private final TemplateEngine templateEngine;
    private final CmsService cmsService;

    @Inject
    public HtmlTemplateRenderer(final UserLanguage userLanguage, final PageDataFactory pageDataFactory, final RequestHookRunner hookRunner,
                                final TemplateEngine templateEngine, final CmsService cmsService) {
        this.userLanguage = userLanguage;
        this.pageDataFactory = pageDataFactory;
        this.hookRunner = hookRunner;
        this.templateEngine = templateEngine;
        this.cmsService = cmsService;
    }

    @Override
    public CompletionStage<Content> render(final PageContent pageContent, final String templateName, @Nullable final String cmsKey) {
        final PageData pageData = pageDataFactory.create(pageContent);
        return hookRunner.waitForComponentsToFinish()
                .thenComposeAsync(unused -> {
                    PageDataReadyHook.runHook(hookRunner, pageData);
                    logFinalPageData(pageData);
                    if (cmsKey != null) {
                        return cmsService.page(cmsKey, userLanguage.locales())
                                .thenApplyAsync(cmsPage -> renderTemplate(templateName, pageData, cmsPage.orElse(null)));
                    } else {
                        return completedFuture(renderTemplate(templateName, pageData, null));
                    }
                }, HttpExecution.defaultContext());
    }

    private Content renderTemplate(final String templateName, final PageData pageData, @Nullable final CmsPage cmsPage) {
        final TemplateContext templateContext = new TemplateContext(pageData, userLanguage.locales(), cmsPage);
        final String html = templateEngine.render(templateName, templateContext);
        return new Html(html);
    }

    private static void logFinalPageData(final PageData pageData) {
        if (pageDataLoggerAsJson.isDebugEnabled()) {
            try {
                final ObjectWriter objectWriter = objectMapper.writer()
                        .withDefaultPrettyPrinter();
                final String formatted = objectWriter.writeValueAsString(pageData);
                pageDataLoggerAsJson.debug(formatted);
            } catch (final Exception e) {
                pageDataLoggerAsJson.error("serialization of " + pageData + " failed.", e);
            }
        }
    }

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        final SimpleModule mod = new SimpleModule("form module");
        mod.addSerializer(Form.class, new StdScalarSerializer<Form>(Form.class){
            @Override
            public void serialize(final Form value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
                gen.writeObject(value.data());
            }
        });
        mapper.registerModule(mod);
        return mapper;
    }
}