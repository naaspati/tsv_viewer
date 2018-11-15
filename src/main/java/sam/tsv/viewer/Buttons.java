package sam.tsv.viewer;

import static sam.fx.helpers.FxButton.button;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import sam.fx.helpers.FxBindings;
import sam.tsv.viewer.tab2.Tab2;

public class Buttons extends HBox {

	private ObjectBinding<Tab2> tab;

	public Buttons(ReadOnlyObjectProperty<Tab> currentTab) {
		super(10);
		this.tab = FxBindings.map(currentTab, t -> (Tab2)t);
		setPadding(new Insets(5, 10, 5, 10));
		setAlignment(Pos.CENTER_RIGHT);
		disableProperty().bind(currentTab.isNull());

		getChildren().addAll(
				button("Add Row", this::addRow),
				button("Remove Row", this::removeRow)
				);

	}
	private void removeRow(Object ignore) {
		tab.get().removeSeletedRows();
	}
	private void addRow(Object ignore) {
		tab.get().addRow();
	}
}
