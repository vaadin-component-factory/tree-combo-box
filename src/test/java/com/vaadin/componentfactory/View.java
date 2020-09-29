package com.vaadin.componentfactory;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class View extends VerticalLayout {

	public View() {
		DepartmentData departmentData = new DepartmentData();
		TreeComboBox<Department> treeComboBox = new TreeComboBox<>(Department::getName);

		treeComboBox.setItems(departmentData.getRootDepartments(), departmentData::getChildDepartments);

		add(treeComboBox);

		treeComboBox.setLabel("Select one");
		
		treeComboBox.setWidth("350px");
		
		treeComboBox.addValueChangeListener(event -> {
			if (event.getValue() !=  null) {
				add(new Span(event.getValue().getName()));
			} else {
				Notification.show("No value");
			}
		});
		
	}
}
