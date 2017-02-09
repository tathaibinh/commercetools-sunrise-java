package com.commercetools.sunrise.common.search.pagination;

import com.commercetools.sunrise.common.contexts.RequestContext;
import com.commercetools.sunrise.common.contexts.RequestScoped;
import com.commercetools.sunrise.common.models.LinkBean;
import com.commercetools.sunrise.common.models.ViewModelFactory;
import io.sphere.sdk.queries.PagedResult;
import play.Configuration;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.LongStream;

import static com.commercetools.sunrise.common.forms.FormUtils.findSelectedValueFromRequest;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@RequestScoped
public class PaginationBeanFactory extends ViewModelFactory<PaginationBean, PagedResult<?>> {

    private static final String CONFIG_DISPLAYED_PAGES = "pop.pagination.displayedPages";
    private static final int DEFAULT_DISPLAYED_PAGES = 6;

    private final int currentPage;
    private final int displayedPages;
    private final PaginationSettings settings;
    private final RequestContext requestContext;

    @Inject
    public PaginationBeanFactory(final Configuration configuration, final PaginationSettings settings,
                                 final RequestContext requestContext) {
        this.settings = settings;
        this.currentPage = findSelectedValueFromRequest(settings, requestContext);
        this.displayedPages = configuration.getInt(CONFIG_DISPLAYED_PAGES, DEFAULT_DISPLAYED_PAGES);
        this.requestContext = requestContext;

    }

    @Override
    protected PaginationBean getViewModelInstance() {
        return new PaginationBean();
    }

    @Override
    public final PaginationBean create(final PagedResult<?> data) {
        return super.create(data);
    }

    @Override
    protected final void initialize(final PaginationBean model, final PagedResult<?> data) {
        fillNextUrl(model, data);
        fillPreviousUrl(model, data);
        fillFirstPage(model, data);
        fillLastPage(model, data);
        fillPages(model, data);
    }

    protected void fillNextUrl(final PaginationBean bean, final PagedResult<?> pagedResult) {
        if (!pagedResult.isLast()) {
            bean.setNextUrl(buildUrlWithPage(settings.getFieldName(), currentPage + 1));
        }
    }

    protected void fillPreviousUrl(final PaginationBean bean, final PagedResult<?> pagedResult) {
        if (!pagedResult.isFirst()) {
            bean.setPreviousUrl(buildUrlWithPage(settings.getFieldName(), currentPage - 1));
        }
    }

    protected void fillFirstPage(final PaginationBean bean, final PagedResult<?> pagedResult) {
        final long totalPages = calculateTotalPages(pagedResult);
        if (firstPageIsDisplayed(totalPages)) {
            bean.setFirstPage(createLinkData(1));
        }
    }

    protected void fillLastPage(final PaginationBean bean, final PagedResult<?> pagedResult) {
        final long totalPages = calculateTotalPages(pagedResult);
        if (lastPageIsDisplayed(totalPages)) {
            bean.setLastPage(createLinkData(totalPages));
        }
    }

    protected void fillPages(final PaginationBean bean, final PagedResult<?> pagedResult) {
        final long totalPages = calculateTotalPages(pagedResult);
        long startPage = 1;
        long endPage = totalPages;
        if (firstPageIsDisplayed(totalPages) && lastPageIsDisplayed(totalPages)) {
            startPage = currentPage - 1;
            endPage = currentPage + 1;
        } else if (firstPageIsDisplayed(totalPages)) {
            startPage = calculateTopThreshold(totalPages);
        } else if (lastPageIsDisplayed(totalPages)) {
            endPage = calculateBottomThreshold();
        }
        bean.setPages(createPages(startPage, endPage));
    }

    private boolean notAllPagesAreDisplayed(final long totalPages) {
        return totalPages > displayedPages;
    }

    private boolean firstPageIsDisplayed(final long totalPages) {
        final boolean currentPageIsAboveBottomThreshold = currentPage > calculateBottomThreshold();
        return notAllPagesAreDisplayed(totalPages) && currentPageIsAboveBottomThreshold;
    }

    private boolean lastPageIsDisplayed(final long totalPages) {
        final boolean currentPageIsBelowTopThreshold = currentPage < calculateTopThreshold(totalPages);
        return notAllPagesAreDisplayed(totalPages) && currentPageIsBelowTopThreshold;
    }

    private List<LinkBean> createPages(final long startPage, final long endPage) {
        return LongStream.rangeClosed(startPage, endPage)
                .mapToObj(this::createLinkData)
                .collect(toList());
    }

    private LinkBean createLinkData(final long page) {
        final LinkBean linkBean = new LinkBean();
        linkBean.setText(String.valueOf(page));
        linkBean.setUrl(buildUrlWithPage(settings.getFieldName(), page));
        if (page == currentPage) {
            linkBean.setSelected(true);
        }
        return linkBean;
    }

    private String buildUrlWithPage(final String key, final long page) {
        return requestContext.buildUrl(key, singletonList(String.valueOf(page)));
    }

    private int calculateBottomThreshold() {
        return displayedPages - 1;
    }

    private long calculateTopThreshold(final long totalPages) {
        return totalPages - displayedPages + 2;
    }

    private long calculateTotalPages(final PagedResult<?> pagedResult) {
        if (pagedResult.isLast()) {
            return currentPage;
        } else {
            final Double totalPages = Math.ceil(pagedResult.getTotal() / pagedResult.getCount());
            return totalPages.longValue();
        }
    }
}
