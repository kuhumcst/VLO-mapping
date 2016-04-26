package eu.clarin.cmdi.vlo.wicket.pages;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import eu.clarin.cmdi.vlo.FacetConstants;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import eu.clarin.cmdi.vlo.config.VloConfig;
import eu.clarin.cmdi.vlo.pojo.QueryFacetsSelection;
import eu.clarin.cmdi.vlo.service.PageParametersConverter;
import eu.clarin.cmdi.vlo.service.solr.FacetFieldsService;
import eu.clarin.cmdi.vlo.wicket.model.FacetFieldsModel;
import eu.clarin.cmdi.vlo.wicket.model.FacetNamesModel;
import eu.clarin.cmdi.vlo.wicket.model.PermaLinkModel;
import eu.clarin.cmdi.vlo.wicket.panels.BreadCrumbPanel;
import eu.clarin.cmdi.vlo.wicket.panels.SingleFacetPanel;
import eu.clarin.cmdi.vlo.wicket.panels.TopLinksPanel;
import eu.clarin.cmdi.vlo.wicket.panels.search.AvailabilityFacetPanel;
import eu.clarin.cmdi.vlo.wicket.panels.search.AdvancedSearchOptionsPanel;
import eu.clarin.cmdi.vlo.wicket.panels.search.FacetPanel;
import eu.clarin.cmdi.vlo.wicket.panels.search.FacetsPanel;
import eu.clarin.cmdi.vlo.wicket.panels.search.SearchFormPanel;
import eu.clarin.cmdi.vlo.wicket.panels.search.SearchResultsPanel;

/**
 * The main search page showing a search form, facets, and search results
 *
 * @author twagoo
 */
public class FacetedSearchPage extends VloBasePage<QueryFacetsSelection> {

    private static final long serialVersionUID = 1L;
    private final static List<String> ADDITIONAL_FACETS = ImmutableList.of(FacetConstants.FIELD_AVAILABILITY);

    @SpringBean
    private FacetFieldsService facetFieldsService;
    @SpringBean
    private VloConfig vloConfig;
    @SpringBean(name = "queryParametersConverter")
    private PageParametersConverter<QueryFacetsSelection> paramsConverter;

    private SearchResultsPanel searchResultsPanel;
    private Component facetsPanel;
    private Component navigation;
    private Component searchForm;
    private Component optionsPanel;
    private Component availabilityFacetPanel;

    private IModel<List<String>> facetNamesModel;
    private FacetFieldsModel fieldsModel;

    public FacetedSearchPage(IModel<QueryFacetsSelection> queryModel) {
        super(queryModel);
        createModels();
        addComponents();
    }

    public FacetedSearchPage(PageParameters parameters) {
        super(parameters);

        final QueryFacetsSelection selection = paramsConverter.fromParameters(parameters);
        final IModel<QueryFacetsSelection> queryModel = new Model<>(selection);
        setModel(queryModel);
        createModels();
        addComponents();
    }

    private void createModels() {
        final List<String> facetFields = vloConfig.getFacetFields();
        final List<String> allFields = ImmutableList.copyOf(Iterables.concat(facetFields, ADDITIONAL_FACETS));
        facetNamesModel = new FacetNamesModel(facetFields);
        fieldsModel = new FacetFieldsModel(facetFieldsService, allFields, getModel(), -1);
    }

    private void addComponents() {
        navigation = createNavigation("navigation");
        add(navigation);

        searchForm = createSearchForm("search");
        add(searchForm);

        facetsPanel = createFacetsPanel("facets");
        add(facetsPanel);

        availabilityFacetPanel = createAvailabilityPanel("availability");
        add(availabilityFacetPanel);

        optionsPanel = createOptionsPanel("options");
        add(optionsPanel);

        searchResultsPanel = new SearchResultsPanel("searchResults", getModel());
        add(searchResultsPanel);
    }

    private WebMarkupContainer createNavigation(String id) {
        final WebMarkupContainer container = new WebMarkupContainer(id);
        container.setOutputMarkupId(true);
        container.add(new BreadCrumbPanel("breadcrumbs", getModel()) {

            @Override
            protected void onSelectionChanged(QueryFacetsSelection selection, AjaxRequestTarget target) {
                setModelObject(selection);
                updateSelection(target);
            }

        });
        container.add(new TopLinksPanel("permalink", new PermaLinkModel(getPageClass(), getModel())) {

            @Override
            protected void onChange(AjaxRequestTarget target) {
                if (target != null) {
                    target.add(container);
                }
            }

        });
        return container;
    }

    private Panel createOptionsPanel(String id) {
        final Panel optionsPanel = new AdvancedSearchOptionsPanel(id, getModel()) {

            @Override
            protected void selectionChanged(AjaxRequestTarget target) {
                updateSelection(target);
            }
        };
        optionsPanel.setOutputMarkupId(true);
        return optionsPanel;
    }

    private Panel createAvailabilityPanel(String id) {
        final Panel availabilityPanel = new AvailabilityFacetPanel(id, getModel(), fieldsModel) {

            @Override
            protected void selectionChanged(AjaxRequestTarget target) {
                updateSelection(target);
            }
        };
        availabilityPanel.setOutputMarkupId(true);
        return availabilityPanel;
    }

    private Panel createSearchForm(String id) {
        final SearchFormPanel form = new SearchFormPanel(id, getModel()) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                // reset expansion state of search results
                searchResultsPanel.resetExpansion();
                updateSelection(target);
            }

        };
        form.setOutputMarkupId(true);
        return form;
    }

    private Panel createFacetsPanel(final String id) {

        final FacetsPanel panel = new FacetsPanel(id, facetNamesModel, fieldsModel, getModel()) {

            @Override
            protected void selectionChanged(AjaxRequestTarget target) {
                updateSelection(target);
            }
        };
        panel.setOutputMarkupId(true);
        return panel;
    }

    private void updateSelection(AjaxRequestTarget target) {

        //detach facetFieldsModel when selection is changed
        fieldsModel.detach();

        // selection changed, update facets and search results
        if (target != null) { // null if JavaScript disabled
            target.add(navigation);
            target.add(searchForm);
            target.add(searchResultsPanel);
            target.add(facetsPanel);
            target.add(optionsPanel);
            target.add(availabilityFacetPanel);
        }
    }

    @Override
    public IModel<String> getCanonicalUrlModel() {
        return new PermaLinkModel(getPageClass(), getModel());
    }
}
