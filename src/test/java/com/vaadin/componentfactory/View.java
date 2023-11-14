package com.vaadin.componentfactory;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class View extends VerticalLayout {

    public View() {
        DepartmentData departmentData = new DepartmentData();
        TreeComboBox<Department> treeComboBox = new TreeComboBox<>(
                Department::getName);

        treeComboBox.setItems(departmentData.getRootDepartments(),
                departmentData::getChildDepartments);

        treeComboBox.setLabel("Select one");

        treeComboBox.setWidth("350px");

        treeComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                add(new Span(event.getValue().getName()));
            } else {
                Notification.show("No value");
            }
        });

        Checkbox leafsOnly = new Checkbox("Leafs Only");
        leafsOnly.addValueChangeListener(e -> {
            treeComboBox.setSelectOnlyLeafs(e.getValue());
        });

        treeComboBox.setIcon(VaadinIcon.INFO.create());
        treeComboBox.setClearButtonVisible(false);
        treeComboBox.setValue(departmentData
                .getChildDepartments(departmentData.getRootDepartments().get(0))
                .get(0));
        treeComboBox.setTooltipText("Tooltip");
        treeComboBox.setHelperText("Helper");

        add(treeComboBox, leafsOnly);

    }
}
