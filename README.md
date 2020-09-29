# Component Factory TreeComboBox for Vaadin 14

This is hierarchical ComboBox type single select component. It currently works with in-memory data
providers, i.e. TreeDataProvider, it supports filtering, but not adding new items on the fly.

# What does the component do?

TreeComboBox allows filtering out items organized in a hierarchy and selecting one.

### Vaadin Prime
This component is part of Vaadin Prime. Still, open source you need to have a valid CVAL license in order to use it. Read more at: vaadin.com/pricing

## Basic Usage
```java
DepartmentData departmentData = new DepartmentData();
TreeComboBox<Department> treeComboBox = new TreeComboBox<>(Department::getName);
treeComboBox.setItems(departmentData.getRootDepartments(), departmentData::getChildDepartments);
```

# How to run the demo?

The Demo can be run by executing the maven goal:

```mvn jetty:run```

After server startup, you'll be able find the demo at [http://localhost:8080/](http://localhost:8080/)


## License & Author

This Add-on is distributed under [Commercial Vaadin Add-on License version 3](http://vaadin.com/license/cval-3) (CVALv3). For license terms, see LICENSE.txt.

Component Factory Enhanced Dialog is written by Vaadin Ltd.


## Setting up for development:

Clone the project in GitHub (or fork it if you plan on contributing)

```
git clone git@github.com:vaadin-component-factory/tree-combo-box.git
```

to install project to your maven repository run
 
```mvn install```