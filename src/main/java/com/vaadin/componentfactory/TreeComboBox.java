package com.vaadin.componentfactory;

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

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
public class TreeComboBox<T> extends Composite<HorizontalLayout>
        implements SingleSelect<TreeComboBox<T>, T>,
        HasHierarchicalDataProvider<T>, Focusable, HasSize, HasElement {

    public enum FilterMode {
        EXACT, STARTS_WITH, CONTAINS, EXACT_CASE, STARTS_WITH_CASE, CONTAINS_CASE
    }

    private ValueProvider<T, String> valueProvider;
    private TreeDataProvider<T> dataProvider;

    private TextField filterField = new TextField();
    private Button openButton = new Button();
    private Popup popup = new Popup();
    private Tree<T> tree = null;
    private FilterMode filterMode = FilterMode.CONTAINS;

    /**
     * Constructs a new TreeComboBox Component.
     * 
     * @param valueProvider
     *            the item caption provider to use, not <code>null</code>
     */
    public TreeComboBox(ValueProvider<T, String> valueProvider) {
        this.valueProvider = valueProvider;
        this.tree = new Tree<>(valueProvider);
        filterField.addValueChangeListener(event -> {
            if (event.isFromClient() && !event.getValue().isEmpty()) {
                popup.setOpened(true);
                dataProvider.setFilter(item -> {
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
            }
        });
        filterField.setClearButtonVisible(true);
        filterField.setAutoselect(true);
        filterField.setValueChangeMode(ValueChangeMode.TIMEOUT);
        filterField.setValueChangeTimeout(1000);
        tree.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                filterField
                        .setValue(this.valueProvider.apply(event.getValue()));
                popup.setOpened(false);
                filterField.focus();
            }
        });

        Registration reg = filterField.addKeyDownListener(Key.ARROW_DOWN,
                event -> {
                    popup.setOpened(true);
                    tree.focus();
                });
        openButton.setIcon(VaadinIcon.CHEVRON_DOWN.create());
        openButton.setId("open-button");
        openButton.addClickListener(event -> {
            popup.setOpened(true);
        });
        popup.setFor("open-button");
        popup.add(tree);
        tree.setHeightByRows(true);
        setWidth("300px");
        getContent().setFlexGrow(1, filterField);
        getContent().setMargin(false);
        getContent().setSpacing(false);
        getContent().setDefaultVerticalComponentAlignment(Alignment.END);
        getContent().add(openButton, filterField, popup);
    }

    private void selectFilteredItem(T item) {
        HierarchicalQuery<T, SerializablePredicate<T>> query = new HierarchicalQuery<>(
                dataProvider.getFilter(), item);
        int size = dataProvider.size(query);
        if (size == 1) {
            dataProvider.fetch(query).findFirst().ifPresent(i -> {
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
        this.dataProvider = (TreeDataProvider<T>) dataProvider;
        tree.setDataProvider(dataProvider);
    }

    @Override
    public HierarchicalDataProvider<T, SerializablePredicate<T>> getDataProvider() {
        return dataProvider;
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

    @Override
    public Registration addValueChangeListener(
            ValueChangeListener<? super ComponentValueChangeEvent<TreeComboBox<T>, T>> listener) {
        return tree.asSingleSelect().addValueChangeListener(
                (ValueChangeListener<? super ComponentValueChangeEvent<Grid<T>, T>>) listener);
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
        this.getContent().setWidth(width);
        tree.setWidth(width);
    }

    @Override
    public void setDataProvider(
            HierarchicalDataProvider<T, ?> hierarchicalDataProvider) {
        tree.setDataProvider(hierarchicalDataProvider);
    }
}
