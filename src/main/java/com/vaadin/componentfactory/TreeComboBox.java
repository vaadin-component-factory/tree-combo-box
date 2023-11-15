package com.vaadin.componentfactory;

import java.util.Random;

/*-
 * #%L
 * TreeComboBox
 * %%
 * Copyright (C) 2020 Vaadin Ltd
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.vaadin.tatu.Tree;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.shared.HasTooltip;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.hierarchy.HasHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.selection.SingleSelect;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoIcon;

/**
 * This is hierarchical ComboBox type single select component. It currently
 * works with in-memory data providers, i.e. TreeDataProvider, it supports
 * filtering, but not adding new items on the fly.
 * 
 * @author Tatu Lund
 *
 * @param <T>
 *            Bean type used in TreeData
 */
@Tag("div")
public class TreeComboBox<T> extends AbstractField<TreeComboBox<T>, T>
        implements SingleSelect<TreeComboBox<T>, T>,
        HasHierarchicalDataProvider<T>, Focusable<TreeComboBox<T>>, HasSize,
        HasElement, HasValidation, HasHelper, HasTooltip {

    public enum FilterMode {
        EXACT, STARTS_WITH, CONTAINS, EXACT_CASE, STARTS_WITH_CASE, CONTAINS_CASE
    }

    private ValueProvider<T, String> valueProvider;

    private Random rand = new Random();
    private TextField filterField = new TextField();
    private Button openButton = new Button();
    private Popup popup = new Popup();
    private Tree<T> tree = null;
    private FilterMode filterMode = FilterMode.CONTAINS;

    private String id;

    /**
     * Constructs a new TreeComboBox Component.
     * 
     * @param valueProvider
     *            the item caption provider to use, not <code>null</code>
     */
    public TreeComboBox(ValueProvider<T, String> valueProvider) {
        super(null);
        this.valueProvider = valueProvider;
        this.tree = new Tree<>(valueProvider);
        filterField.addValueChangeListener(event -> {
            if (event.isFromClient() && !event.getValue().isEmpty()) {
                popup.setOpened(true);
                ((TreeDataProvider<T>) getDataProvider()).setFilter(item -> {
                    switch (filterMode) {
                    case EXACT_CASE:
                        return this.valueProvider.apply(item)
                                .equals(event.getValue());
                    case STARTS_WITH_CASE:
                        return this.valueProvider.apply(item)
                                .startsWith(event.getValue());
                    case CONTAINS_CASE:
                        return this.valueProvider.apply(item)
                                .contains(event.getValue());
                    case EXACT:
                        return this.valueProvider.apply(item).toLowerCase()
                                .equals(event.getValue().toLowerCase());
                    case STARTS_WITH:
                        return this.valueProvider.apply(item).toLowerCase()
                                .startsWith(event.getValue().toLowerCase());
                    case CONTAINS:
                        return this.valueProvider.apply(item).toLowerCase()
                                .contains(event.getValue().toLowerCase());
                    }
                    return this.valueProvider.apply(item).toLowerCase()
                            .contains(event.getValue().toLowerCase());
                });
                selectFilteredItem(null);
            } else if (event.isFromClient()) {
                if (getValue() != null) {
                    tree.deselect(getValue());
                }
                ((TreeDataProvider<T>) getDataProvider()).setFilter(null);
            }
        });
        filterField.setClearButtonVisible(true);
        filterField.setAutoselect(true);
        filterField.setValueChangeMode(ValueChangeMode.TIMEOUT);
        filterField.setValueChangeTimeout(1000);
        filterField.getElement().executeJs(
                "this.inputElement.setAttribute('autocomplete','off')");
        tree.addSelectionListener(event -> {
            if (!event.getSelected().isEmpty()) {
                T value = event.getSelected().stream().findFirst().get();
                filterField.setValue(this.valueProvider.apply(value));
                popup.setOpened(false);
                filterField.focus();
                fireEvent(new ComponentValueChangeEvent<>(this, this, value,
                        event.isFromClient()));
            }
        });

        Registration reg = filterField.addKeyDownListener(Key.ARROW_DOWN,
                event -> {
                    popup.setOpened(true);
                    tree.focus();
                });
        openButton.setIcon(LumoIcon.ANGLE_DOWN.create());
        openButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        openButton.addClickListener(event -> {
            popup.setOpened(true);
        });
        Div popupTarget = new Div();
        popupTarget.setHeightFull();
        id = randomId("open-button", 9);
        popupTarget.setId(id);
        popup.setFor(id);
        popup.add(tree);
        tree.setHeightByRows(true);
        filterField.setPrefixComponent(popupTarget);
        filterField.setSuffixComponent(openButton);
        setWidth("300px");
        getElement().appendChild(filterField.getElement());
        getElement().appendChild(popup.getElement());
    }

    private void selectFilteredItem(T item) {
        HierarchicalQuery<T, SerializablePredicate<T>> query = new HierarchicalQuery<>(
                ((TreeDataProvider<T>) getDataProvider()).getFilter(), item);
        int size = getDataProvider().size(query);
        if (size == 1) {
            getDataProvider().fetch(query).findFirst().ifPresent(i -> {
                tree.expand(i);
                selectFilteredItem(i);
            });

        } else if (size == 0) {
            tree.select(item);
        }
    }

    /**
     * Constructs a new TreeComboBox Component with given caption and
     * {@code TreeData}.
     *
     * @param valueProvider
     *            the item caption provider to use, not <code>null</code>
     * @param treeData
     *            the tree data for component
     */
    public TreeComboBox(TreeData<T> treeData,
            ValueProvider<T, String> valueProvider) {
        this(new TreeDataProvider<>(treeData), valueProvider);
    }

    /**
     * Constructs a new TreeComboBox Component with given caption and
     * {@code HierarchicalDataProvider}.
     *
     * @param valueProvider
     *            the item caption provider to use, not <code>null</code>
     * @param dataProvider
     *            the hierarchical data provider for component
     */
    public TreeComboBox(HierarchicalDataProvider<T, ?> dataProvider,
            ValueProvider<T, String> valueProvider) {
        this(valueProvider);

        setDataProvider(dataProvider);
    }

    @Override
    public void setValue(T value) {
        tree.asSingleSelect().setValue(value);
    }

    @Override
    public T getValue() {
        return tree.asSingleSelect().getValue();
    }

    @Override
    public void setDataProvider(DataProvider<T, ?> dataProvider) {
        if (!(dataProvider instanceof TreeDataProvider)) {
            throw new IllegalArgumentException(
                    "DataProvider needs to be TreeDataProvider");
        }
        tree.setDataProvider(dataProvider);
    }

    @Override
    public HierarchicalDataProvider<T, SerializablePredicate<T>> getDataProvider() {
        return tree.getDataProvider();
    }

    @Override
    public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
        filterField.setRequiredIndicatorVisible(requiredIndicatorVisible);
    }

    @Override
    public boolean isRequiredIndicatorVisible() {
        return filterField.isRequiredIndicatorVisible();
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        filterField.setReadOnly(readOnly);
        openButton.setEnabled(!readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return filterField.isReadOnly();
    }

    @Override
    public void focus() {
        filterField.focus();
    }

    /**
     * String used for the label element.
     *
     * @param label
     *            the String value to set
     */
    public void setLabel(String label) {
        filterField.setLabel(label);
    }

    /**
     * Sets the width of the component.
     * <p>
     * The width should be in a format understood by the browser, e.g. "100px"
     * or "2.5em".
     * <p>
     * If the provided {@code width} value is {@literal null} then width is
     * removed.
     * <p>
     * Note: The content in popup needs an explicit width, hence "100%" does not
     * work.
     * 
     * @param width
     *            the width to set, may be {@code null}
     */
    @Override
    public void setWidth(String width) {
        filterField.setWidth(width);
        tree.setWidth(width);
    }

    /**
     * Sets the width of the Popup part.
     * <p>
     * Note: If setWidth is used, the Popup width be reset to width defined by
     * setWidth.
     * 
     * @param width
     *            the width to be set
     */
    public void setPopupWidth(String width) {
        tree.setWidth(width);
    }

    @Override
    public void setDataProvider(
            HierarchicalDataProvider<T, ?> hierarchicalDataProvider) {
        tree.setDataProvider(hierarchicalDataProvider);
    }

    @Override
    protected void setPresentationValue(T newPresentationValue) {
        tree.select(newPresentationValue);
    }

    /**
     * When true allow selecting only the leaf nodes
     * 
     * @param selectOnlyLeafs
     *            boolean value.
     */
    public void setSelectOnlyLeafs(boolean selectOnlyLeafs) {
        tree.setSelectOnlyLeafs(selectOnlyLeafs);
    }

    /**
     * Set icon as prefix component of the field.
     * 
     * @param component
     *            Preferably a icon component
     */
    public void setIcon(Component component) {
        component.setId(id);
        filterField.setPrefixComponent(component);
    }

    /**
     * When false clear button will not be visible, default true.
     * 
     * @param clearButtonVisible
     *            boolean value.
     */
    public void setClearButtonVisible(boolean clearButtonVisible) {
        filterField.setClearButtonVisible(clearButtonVisible);
    }

    /**
     * When set to true, user cannot type into input field, only select values
     * from the popup.
     * 
     * @param disableFiltering
     *            boolean value
     */
    public void setDisableFiltering(boolean disableFiltering) {
        if (disableFiltering) {
            filterField.getElement().executeJs(
                    "this.inputElement.setAttribute('readonly', true);");
        } else {
            filterField.getElement().executeJs(
                    "this.inputElement.removeAttribute('readonly');");
        }
    }

    private String randomId(String prefix, int chars) {
        int limit = (int) (Math.pow(10, chars) - 1);
        String key = "" + rand.nextInt(limit);
        key = String.format("%" + chars + "s", key).replace(' ', '0');
        return prefix + "-" + key;
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        filterField.setErrorMessage(errorMessage);
    }

    @Override
    public String getErrorMessage() {
        return filterField.getErrorMessage();
    }

    @Override
    public void setInvalid(boolean invalid) {
        filterField.setInvalid(invalid);
    }

    @Override
    public boolean isInvalid() {
        return filterField.isInvalid();
    }

    @Override
    public void setHelperText(String helperText) {
        filterField.setHelperText(helperText);
    }

    @Override
    public String getHelperText() {
        return filterField.getHelperText();
    }

    @Override
    public void setHelperComponent(Component helperComponent) {
        filterField.setHelperComponent(helperComponent);
    }

    @Override
    public Component getHelperComponent() {
        return filterField.getHelperComponent();
    }

    @Override
    public Tooltip setTooltipText(String tooltipText) {
        return filterField.setTooltipText(tooltipText);
    }

    @Override
    public Tooltip getTooltip() {
        return filterField.getTooltip();
    }

}