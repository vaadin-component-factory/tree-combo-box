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

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
        HasHierarchicalDataProvider<T>, Focusable, HasSize, HasElement {

    public enum FilterMode {
        EXACT, STARTS_WITH, CONTAINS, EXACT_CASE, STARTS_WITH_CASE, CONTAINS_CASE
    }

    private ValueProvider<T, String> valueProvider;

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
        openButton.setId("open-button");
        openButton.addClickListener(event -> {
            popup.setOpened(true);
        });
        popup.setFor("open-button");
        popup.add(tree);
        tree.setHeightByRows(true);
        filterField.setPrefixComponent(openButton);
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
}
