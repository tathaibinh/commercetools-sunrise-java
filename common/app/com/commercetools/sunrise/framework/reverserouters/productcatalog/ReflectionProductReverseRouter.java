package com.commercetools.sunrise.framework.reverserouters.productcatalog;

import com.commercetools.sunrise.framework.reverserouters.AbstractReflectionReverseRouter;
import com.commercetools.sunrise.framework.reverserouters.ParsedRouteList;
import com.commercetools.sunrise.framework.reverserouters.ReverseCaller;
import play.mvc.Call;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
final class ReflectionProductReverseRouter extends AbstractReflectionReverseRouter implements ProductSimpleReverseRouter {

    private final ReverseCaller productDetailPageCaller;
    private final ReverseCaller productOverviewPageCaller;
    private final ReverseCaller searchProcessCaller;

    @Inject
    private ReflectionProductReverseRouter(final ParsedRouteList parsedRouteList) {
        productDetailPageCaller = getCallerForRoute(parsedRouteList, PRODUCT_DETAIL_PAGE);
        productOverviewPageCaller = getCallerForRoute(parsedRouteList, PRODUCT_OVERVIEW_PAGE);
        searchProcessCaller = getCallerForRoute(parsedRouteList, SEARCH_PROCESS);
    }

    @Override
    public Call productDetailPageCall(final String languageTag, final String productSlug, final String sku) {
        return productDetailPageCaller.call(languageTag, productSlug, sku);
    }

    @Override
    public Call productOverviewPageCall(final String languageTag, final String categorySlug) {
        return productOverviewPageCaller.call(languageTag, categorySlug);
    }

    @Override
    public Call searchProcessCall(final String languageTag) {
        return searchProcessCaller.call(languageTag);
    }
}