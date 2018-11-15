package sam.tsv.viewer.tab2;

import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.Event;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import sam.tsv.Row;
import sam.tsv.viewer.App;

public class TableCol extends TableColumn<Row, String> {
	public static enum ColEdit {
		PASTE, DELETE
	}

	final int index;
	private int editCount;

	public TableCol(String title, int index) {
		super(title);
		this.index = index;

		setCellFactory(column -> new TableCell2());
		setCellValueFactory(cdata -> new ObservableRow(cdata.getValue(), index));
	}

	private String paste;
	private ColEdit edit;

	public void fastEdit(ColEdit edit, int row, String data) {
		this.edit = edit;
		switch (edit) {
			case DELETE:
				break;
			case PASTE:
				this.paste = Objects.requireNonNull(paste);
				break;
		}
		getTableView().edit(row, this);
	}

	public class ObservableRow extends SimpleStringProperty {
		private final Row row;

		public ObservableRow(Row row, int index) {
			super(row.get(index));
			this.row = row;
		}
		@Override
		public void set(String newValue) {
			if(!Objects.equals(getValue(), newValue)) {
				editCount++;
				row.set(index, newValue);
			}
			super.set(newValue);
		}
	}
	public class TableCell2 extends TableCell<Row, String> {
		private Editor editor;
		@Override
		public void startEdit() {
			if (! isEditable()
					|| ! getTableView().isEditable()
					|| ! getTableColumn().isEditable()) {
				return;
			}

			super.startEdit();

			if(isEditing()) {

				if(edit != null) {
					switch (edit) {
						case DELETE:
							this.commitEdit(null);
							break;
						case PASTE:
							this.commitEdit(paste);
							break;
					}
					paste = null;
					edit = null;
					return;
				} else {
					editor = App.getInstance().editor();
					editor.start(this);	
				}
			}
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void commitEdit(String newValue) {
			if (!isEditing()) return;

			final TableView<Row> table = getTableView();
			if (table != null) {
				// Inform the TableView of the edit being ready to be committed.
				CellEditEvent editEvent = new CellEditEvent(
						table,
						table.getEditingCell(),
						TableColumn.editCommitEvent(),
						newValue
						);

				Event.fireEvent(getTableColumn(), editEvent);
			}

			super.commitEdit(newValue);
			updateItem(newValue, false);

			if (table != null) {
				table.edit(-1, null);
				table.requestFocus();
			}
			if(editor != null) {
				editor.close();
				editor = null;
			}
		}
		@Override
		public void cancelEdit() {
			super.cancelEdit();
			editor.close();
			editor = null;
		}
		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			setText(empty ? null : item);
		}


	}
	public boolean isModified() {
		return editCount != 0;
	}
	public void clearModified() {
		editCount = 0;
	}
}
